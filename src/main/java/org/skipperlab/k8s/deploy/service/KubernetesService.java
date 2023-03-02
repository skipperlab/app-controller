package org.skipperlab.k8s.deploy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import org.skipperlab.k8s.deploy.model.StatusType;
import org.skipperlab.k8s.deploy.model.Workload;
import org.skipperlab.k8s.deploy.repository.WorkloadRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KubernetesService {
    @Value("${kubernetes.upload-dir:./yaml}")
    private String UPLOAD_DIR;
    private final WorkloadRepository workloadRepository;
    private final KubernetesClient kubernetesClient;
    private final Configuration freeMakerCfg;
    private ObjectMapper objectMapper;

    public KubernetesService(WorkloadRepository workloadRepository, KubernetesClient kubernetesClient, Configuration configuration) {
        this.workloadRepository = workloadRepository;
        this.kubernetesClient = kubernetesClient;
        this.freeMakerCfg = configuration;
        this.objectMapper = new ObjectMapper();
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
        String namespace = workload.getNameSpace();

        String existNamespace = this.getNamespace(namespace);
        if(existNamespace == null) {
            Namespace ns = new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(workload.getNameSpace())
                    .addToLabels("app", workload.getName())
                    .endMetadata()
                    .build();
            this.kubernetesClient.namespaces().createOrReplace(ns);
        }

        String yaml = this.getDeploymentYaml(workload);
        InputStream in = new ByteArrayInputStream(yaml.getBytes());

        List<HasMetadata> newResources = this.kubernetesClient.load(in).get();
        if (newResources != null) {
            newResources.forEach(resource -> {
                HasMetadata result = this.kubernetesClient.resource(resource).inNamespace(namespace).createOrReplace();
                if("Deployment".equals(result.getKind())) {
                    workload.setStatus(StatusType.Deployed);
                }
            });
            return workload;
        } else return null;
    }

    protected String getDeploymentYaml(Workload workload) {
        Map<String, Object> root = new HashMap<>();
        this.getConfig(workload.getWorkspace().getConfig(), root);
        this.getConfig(workload.getConfig(), root);

        String fileName = UPLOAD_DIR + "/" + workload.getPalette().getYamlPath();
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Reader in = new InputStreamReader(inputStream);

        Writer out = new StringWriter();
        Template template = null;
        try {
            template = new Template(workload.getName(), in, this.freeMakerCfg);
            template.process(root, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return out.toString();
    }

    private void getConfig(String configStr, Map<String, Object> root) {
        try {
            Iterable<JsonNode> configs = this.objectMapper.readTree(configStr);
            for (JsonNode config : configs) {
                String fieldName = config.fieldNames().next();
                root.put(fieldName,config.get(fieldName).asText());
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Workload deleteDeployment(Workload workload) {
        String namespace = workload.getNameSpace();

        String yaml = this.getDeploymentYaml(workload);
        InputStream in = new ByteArrayInputStream(yaml.getBytes());
        List<HasMetadata> newResources = this.kubernetesClient.load(in).get();
        if (newResources != null) {
            newResources.forEach(resource -> {
                List<StatusDetails> result = this.kubernetesClient.resource(resource).inNamespace(namespace).delete();
                if(result.size() > 0 && "Deployment".equals(result.get(0).getKind())) {
                    workload.setStatus(StatusType.Define);
                }
            });
        }

        String existNamespace = this.getNamespace(namespace);
        if(existNamespace != null) {
            this.kubernetesClient.namespaces().withName(namespace).delete();
        }

        return workload;
    }

    protected Workload fromDeployment(Deployment deployment) {
        Workload workload = workloadRepository.findByName(deployment.getMetadata().getName());
        if(workload != null) {
            workload.setStatus(
                    (deployment.getStatus().getReadyReplicas() == deployment.getStatus().getReplicas()
                    ? StatusType.Running : StatusType.Deployed)
            );
        }
        return workload;
    }

    public String getNamespace(String name) {
        Namespace namespace = this.kubernetesClient.namespaces().withName(name).get();
        if(namespace != null) return name;
        else return null;
    }
}
