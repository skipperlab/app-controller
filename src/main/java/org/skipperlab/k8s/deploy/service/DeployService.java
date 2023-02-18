package org.skipperlab.k8s.deploy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.skipperlab.k8s.deploy.model.StatusType;
import org.skipperlab.k8s.deploy.model.Topic;
import org.skipperlab.k8s.deploy.model.Workload;
import org.skipperlab.k8s.deploy.model.Workspace;
import org.skipperlab.k8s.deploy.repository.TopicRepository;
import org.skipperlab.k8s.deploy.repository.WorkloadRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DeployService {
    private final KafkaService kafkaService;
    private final KubernetesService kubernetesService;
    private final WorkloadRepository workloadRepository;
    private final TopicRepository topicRepository;
    private ObjectMapper objectMapper;

    public DeployService(KafkaService kafkaService, KubernetesService kubernetesService, WorkloadRepository workloadRepository, TopicRepository topicRepository) {
        this.kafkaService = kafkaService;
        this.kubernetesService = kubernetesService;
        this.workloadRepository = workloadRepository;
        this.topicRepository = topicRepository;
        this.objectMapper = new ObjectMapper();
    }

    public Workspace doDeploy(Workspace workspace) {
        Workspace existWorkspace = this.getDeploy(workspace);
        if (Arrays.asList(new StatusType[]{StatusType.Define, StatusType.Deployed})
                .contains(existWorkspace.getStatus()))
        {
            List<Topic> topics = this.getTopics(workspace.getContext());
            topics.forEach(topic -> {
                try {
                    this.kafkaService.createTopic(topic);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            List<Workload> workloads = this.getWorkloads(workspace.getContext());
            workloads.forEach(workload -> {
                try {
                    Workload newWorkload = this.kubernetesService.createDeployment(workload);
                    if(newWorkload != null) {
                        this.workloadRepository.save(newWorkload);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return workspace;
    }

    private List<Topic> getTopics(String context) {
        List<Topic> result = new ArrayList<>();

        JsonNode jsonNode = this.readContext(context);
        jsonNode.withArray("topics").forEach(topic -> {
            String name = topic.get("name").asText();
            result.add(this.topicRepository.findByName(name));
        });

        return result;
    }

    private List<Workload> getWorkloads(String context) {
        List<Workload> result = new ArrayList<>();

        JsonNode jsonNode = this.readContext(context);
        jsonNode.withArray("workloads").forEach(topic -> {
            String name = topic.get("name").asText();
            result.add(this.workloadRepository.findByName(name));
        });

        return result;
    }

    private JsonNode readContext(String context) {
        try {
            return this.objectMapper.readTree(context);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Workspace undoDeploy(Workspace workspace) {
        Workspace existWorkspace = this.getDeploy(workspace);
        if (Arrays.asList(new StatusType[]{StatusType.Deployed, StatusType.Running, StatusType.Done, StatusType.Error})
                .contains(existWorkspace.getStatus()))
        {
            //ToDo - Workload 삭제
            //ToDo - Topic 삭제
        }
        return null;
    }

    public Workspace setDeploy(Workspace workspace) {
        //ToDo
        Workspace existWorkspace = this.getDeploy(workspace);
        return null;
    }

    public Workspace getDeploy(Workspace workspace) {
        String namespace = this.kubernetesService.getNamespace(workspace.getName());
        if(namespace == null) {
            workspace.setStatus(StatusType.Define);
        } else if(workspace.getStatus() == StatusType.Define) {
            workspace.setStatus(StatusType.Deployed);
        }
        return workspace;
    }
}
