package org.skipperlab.k8s.deploy.service;

import org.skipperlab.k8s.deploy.model.Workspace;
import org.springframework.stereotype.Service;

@Service
public class DeployService {
    private final KafkaService kafkaService;
    private final KubernetesService kubernetesService;

    public DeployService(KafkaService kafkaService, KubernetesService kubernetesService) {
        this.kafkaService = kafkaService;
        this.kubernetesService = kubernetesService;
    }

    public Workspace doDeploy(Workspace workspace) {
        //ToDo
        return null;
    }

    public Workspace undoDeploy(Workspace workspace) {
        //ToDo
        return null;
    }

    public Workspace setDeploy(Workspace workspace) {
        //ToDo
        return null;
    }

    public Workspace getDeploy(Workspace workspace) {
        //ToDo
        return null;
    }
}
