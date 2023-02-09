package org.skipperlab.k8s.deploy.controller;

import org.skipperlab.k8s.deploy.model.Workload;
import org.skipperlab.k8s.deploy.service.KubernetesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kubernetes")
public class KubernetesController {

    private final KubernetesService kubernetesAdmin;

    public KubernetesController(KubernetesService kubernetesAdmin) {
        this.kubernetesAdmin = kubernetesAdmin;
    }

    @GetMapping("/workloads")
    public List<Workload> allWorkloads() {
        return this.kubernetesAdmin.allDeployments();
    }

    @GetMapping("/workloads/{name}")
    public Workload getWorkload(@PathVariable String name) {
        return this.kubernetesAdmin.getDeployment(name);
    }
}