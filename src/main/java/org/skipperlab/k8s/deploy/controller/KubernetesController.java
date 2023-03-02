package org.skipperlab.k8s.deploy.controller;

import org.skipperlab.k8s.deploy.model.Workload;
import org.skipperlab.k8s.deploy.service.KubernetesService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/kubernetes")
public class KubernetesController {
    @Value("${kubernetes.upload-dir:./yaml}")
    private String UPLOAD_DIR;
    private final KubernetesService kubernetesAdmin;

    public KubernetesController(KubernetesService kubernetesAdmin) {
        this.kubernetesAdmin = kubernetesAdmin;
    }

    @PostMapping("/yaml")
    public String uploadYamlFile(@RequestParam("file") MultipartFile file) {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create upload directory", e);
            }
        }
        try {
            byte[] bytes = file.getBytes();
            Path filePath = Paths.get(UPLOAD_DIR + "/" + file.getOriginalFilename());
            Files.write(filePath, bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }

        return "File uploaded successfully";
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