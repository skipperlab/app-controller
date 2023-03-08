package org.skipperlab.k8s.deploy.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.skipperlab.k8s.deploy.model.CommandType;
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
    public JsonNode getStatues(@PathVariable Long id) {
        Optional<Workspace> workspace = this.workspaceRepository.findById(id);
        if(workspace.isPresent()) {
            return this.deployService.getStatus(workspace.get());
        } else {
            throw new ResourceNotFoundException();
        }
    }

    @PostMapping("/status/{id}")
    public JsonNode setStatus(@PathVariable Long id, @RequestBody JsonNode body) {
        Optional<Workspace> workspace = this.workspaceRepository.findById(id);
        if(workspace.isPresent()) {
            String command = body.get("command").asText("Status");
            return this.deployService.setStatus(workspace.get(), CommandType.valueOf(command));
        } else {
            throw new ResourceNotFoundException();
        }
    }

    @PostMapping("/do/{id}")
    public Workspace doDeploy(@PathVariable Long id, @RequestBody JsonNode config) {
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