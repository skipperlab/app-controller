package org.skipperlab.k8s.deploy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.skipperlab.k8s.deploy.model.*;
import org.skipperlab.k8s.deploy.repository.TopicRepository;
import org.skipperlab.k8s.deploy.repository.WorkloadRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        Workspace existWorkspace = this.getStatus(workspace);
        if (Arrays.asList(new StatusType[]{StatusType.Define, StatusType.Deployed})
                .contains(existWorkspace.getStatus()))
        {
            List<Workload> workloads = this.workloadRepository.findByWorkspaceId(workspace.getId());
            List<Topic> topics = this.getTopics(workloads);
            topics.forEach(topic -> {
                try {
                    this.kafkaService.createTopic(topic);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
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

    private List<Topic> getTopics(List<Workload> workloads) {
        List<Topic> result = workloads.stream()
                .flatMap(workload -> {
                    try {
                        return Stream.concat(
                                Arrays.stream(objectMapper.readValue(workload.getInputTopics(), String[].class)),
                                Arrays.stream(objectMapper.readValue(workload.getOutputTopics(), String[].class))
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).distinct()
                .map(topicName -> this.topicRepository.findByName(topicName))
                .collect(Collectors.toList());
        return result;
    }

    public Workspace undoDeploy(Workspace workspace) {
        Workspace existWorkspace = this.getStatus(workspace);
        if (Arrays.asList(new StatusType[]{StatusType.Deployed, StatusType.Running, StatusType.Stop, StatusType.Done, StatusType.Error})
                .contains(existWorkspace.getStatus()))
        {
            List<Workload> workloads = this.workloadRepository.findByWorkspaceId(workspace.getId());
            workloads.forEach(workload -> {
                try {
                    Workload oldWorkload = this.kubernetesService.deleteDeployment(workload);
                    if(oldWorkload != null) {
                        this.workloadRepository.save(oldWorkload);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return null;
    }

    public Workspace setStatus(Workspace workspace, CommandType commandType) {
        Workspace existWorkspace = this.getStatus(workspace);
        if (Arrays.asList(new StatusType[]{StatusType.Deployed, StatusType.Running, StatusType.Stop, StatusType.Done, StatusType.Error})
                .contains(existWorkspace.getStatus()))
        {
            List<Workload> workloads = this.workloadRepository.findByWorkspaceId(workspace.getId());
            workloads.forEach(workload -> {
                try {
                    Workload existWorkload = this.kubernetesService.getDeployment(workload.getName(), workload.getNameSpace());
                    if(existWorkload != null) {
                        this.workloadRepository.save(existWorkload);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return null;
    }

    public Workspace getStatus(Workspace workspace) {
        String namespace = this.kubernetesService.getNamespace(workspace.getName());
        if(namespace == null) {
            workspace.setStatus(StatusType.Define);
        } else if(workspace.getStatus() == StatusType.Define) {
            workspace.setStatus(StatusType.Deployed);
        }
        return workspace;
    }
}
