package org.skipperlab.k8s.deploy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.fabric8.kubernetes.api.model.*;
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
        Map<String, Object> root = this.getRootFromWorkload(workload);
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
            template = new Template("WORK_" +workload.getId().toString(), in, this.freeMakerCfg);
            template.process(root, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return out.toString();
    }

    public Map<String, Object> getRootFromWorkload(Workload workload) {
        return new HashMap<>(){{
            put("name", workload.getName());
            put("inputTopic", workload.getInputTopics());
            put("outputTopic", workload.getOutputTopics());
        }};
    }

    public void getConfig(String configStr, Map<String, Object> root) {
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

    public String getServiceUrl(Workload workload) {
        io.fabric8.kubernetes.api.model.Service service = this.kubernetesClient.services()
                .inNamespace(workload.getNameSpace())
                .withName(workload.getName()).get();
        if(service != null) {
            switch (service.getSpec().getType()) {
                case "LoadBalancer":
                    return service.getStatus().getLoadBalancer().getIngress().get(0).getIp() + ":" + service.getSpec().getPorts().get(0).getPort();
                case "NodePort":
                    Deployment deployment = this.kubernetesClient.apps().deployments()
                            .inNamespace(workload.getNameSpace())
                            .withName(workload.getName()).get();
                    if (deployment != null) {
                        List<Pod> podList = kubernetesClient.pods()
                                .inNamespace(workload.getNameSpace())
                                .withLabels(deployment.getSpec().getSelector().getMatchLabels())
                                .list().getItems();
                        if (podList.size() > 0) {
                            Node node = this.kubernetesClient.nodes().withName(podList.get(0).getSpec().getNodeName()).get();
                            if (node != null) {
                                return node.getStatus().getAddresses().get(0).getAddress() + ":" + service.getSpec().getPorts().get(0).getNodePort();
                            }
                        }
                    }
                default:
                    return service.getSpec().getClusterIP() + ":" + service.getSpec().getPorts().get(0).getPort();
            }
        } else {
            Deployment deployment = this.kubernetesClient.apps().deployments()
                    .inNamespace(workload.getNameSpace())
                    .withName(workload.getName()).get();
            if (deployment != null) {
                String selector = deployment.getSpec().getSelector().getMatchLabels().toString();
                Map<String, String> matchLabels = this.parseSelector(selector);
                PodList podList = this.kubernetesClient.pods()
                        .inNamespace(workload.getNameSpace())
                        .withLabels(matchLabels).list();
                if (podList != null && podList.getItems().size() > 0) {
                    Pod pod = podList.getItems().get(0);
                    List<Container> containerList = pod.getSpec().getContainers();
                    if (containerList != null && containerList.size() > 0) {
                        Container container = containerList.get(0);
                        List<ContainerPort> portList = container.getPorts();
                        if (portList != null && portList.size() > 0) {
                            ContainerPort port = portList.get(0);
                            return pod.getStatus().getPodIP() + ":" + port.getContainerPort();
                        }
                    }
                }
            }
        }
        return null;
    }

    private Map<String, String> parseSelector(String selector) {
        Map<String, String> matchLabels = new HashMap<>();
        String[] selectorParts = selector.substring(1, selector.length() - 1).split(",");
        for (String part : selectorParts) {
            String[] labelParts = part.trim().split("=");
            if (labelParts.length == 2) {
                matchLabels.put(labelParts[0], labelParts[1].replaceAll("'", ""));
            }
        }
        return matchLabels;
    }
}
