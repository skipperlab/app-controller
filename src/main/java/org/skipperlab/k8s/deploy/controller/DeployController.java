package org.skipperlab.k8s.deploy.controller;

import org.skipperlab.k8s.deploy.model.Workspace;
import org.skipperlab.k8s.deploy.repository.WorkspaceRepository;
import org.skipperlab.k8s.deploy.service.DeployService;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/deploy")
public class DeployController {
    private final WorkspaceRepository workspaceRepository;
    private final DeployService deployService;

    public DeployController(WorkspaceRepository workspaceRepository, DeployService deployService) {
        this.workspaceRepository = workspaceRepository;
        this.deployService = deployService;
    }

    @GetMapping("/status/{id}")
    public Workspace getStatues(@PathVariable Long id) {
        Optional<Workspace> workspace = this.workspaceRepository.findById(id);
        if(workspace.isPresent()) {
            return this.deployService.getDeploy(workspace.get());
        } else {
            throw new ResourceNotFoundException();
        }
    }

    @PostMapping("/status/{id}")
    public Workspace setStatus(@PathVariable Long id) {
        Optional<Workspace> workspace = this.workspaceRepository.findById(id);
        if(workspace.isPresent()) {
            return this.deployService.setDeploy(workspace.get());
        } else {
            throw new ResourceNotFoundException();
        }
    }

    @PostMapping("/do/{id}")
    public Workspace doDeploy(@PathVariable Long id) {
        Optional<Workspace> workspace = this.workspaceRepository.findById(id);
        if(workspace.isPresent()) {
            return this.deployService.doDeploy(workspace.get());
        } else {
            throw new ResourceNotFoundException();
        }
    }

    @DeleteMapping("/do/{id}")
    public Workspace undoDeploy(@PathVariable Long id) {
        Optional<Workspace> workspace = this.workspaceRepository.findById(id);
        if(workspace.isPresent()) {
            return this.deployService.undoDeploy(workspace.get());
        } else {
            throw new ResourceNotFoundException();
        }
    }
}