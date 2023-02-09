package org.skipperlab.k8s.deploy.service;

import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import org.skipperlab.k8s.deploy.model.*;
import org.skipperlab.k8s.deploy.repository.PaletteRepository;
import org.skipperlab.k8s.deploy.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KubernetesService {

    private final PaletteRepository paletteRepository;
    private final WorkspaceRepository workspaceRepository;
    private final KubernetesClient kubernetesClient;

    public KubernetesService(PaletteRepository paletteRepository, WorkspaceRepository workspaceRepository, KubernetesClient kubernetesClient) {
        this.paletteRepository = paletteRepository;
        this.workspaceRepository = workspaceRepository;
        this.kubernetesClient = kubernetesClient;
    }

    public List<Workload> allDeployments() {
        return this.kubernetesClient.apps().deployments().list().getItems().stream()
                .map(deployment -> this.fromDeployment(deployment))
                .collect(Collectors.toList());
    }

    public Workload getDeployment(String name) {
        return getDeployment(name, null);
    }

    public Workload getDeployment(String name, String nameSpace) {
        RollableScalableResource<Deployment> existWorkload = this.kubernetesClient.apps().deployments()
                .inNamespace(nameSpace)
                .withName(name);
        if(existWorkload != null) {
            return this.fromDeployment(existWorkload.get());
        } else return null;
    }

    public Workload createDeployment(Workload workload) {
        RollableScalableResource<Deployment> createDeployment = this.kubernetesClient.apps().deployments()
                .inNamespace(workload.getNameSpace())
                .load(this.getDeploymentYaml(workload));
        if (createDeployment != null) {
            Map<String, String> labels = createDeployment.get().getMetadata().getLabels();
            labels.put("palette", workload.getPalette().getName());
            labels.put("kafkaType", workload.getKafkaType().toString());
            createDeployment.get().getMetadata().setLabels(labels);

            createDeployment.createOrReplace();
            return this.fromDeployment(createDeployment.get());
        } else return null;
    }

    protected String getDeploymentYaml(Workload workload) {
        String palette = workload.getPalette().getContext();
        return null;
    }

    public Workload deleteDeployment(Workload workload) {
        RollableScalableResource<Deployment> existWorkload = this.kubernetesClient.apps().deployments()
                .inNamespace(workload.getNameSpace())
                .withName(workload.getName());
        if(existWorkload != null) {
            List<StatusDetails> deleteResults = existWorkload.delete();

            workload.setStatus(StatusType.Define);
            return workload;
        } else return null;
    }

    protected Workload fromDeployment(Deployment deployment) {
        Palette palette = paletteRepository.findByName(deployment.getMetadata().getLabels().get("palette"));
        Workspace workspace = workspaceRepository.findByName(deployment.getMetadata().getNamespace());
        return new Workload(null,
                palette,
                KafkaType.valueOf(deployment.getMetadata().getLabels().get("kafkaType")),
                deployment.getMetadata().getName(),
                workspace,
                (deployment.getStatus().getReadyReplicas()==deployment.getStatus().getReplicas()
                        ? StatusType.Running : StatusType.Deployed),
                null
        );
    }
}
