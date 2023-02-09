package org.skipperlab.k8s.deploy.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.skipperlab.k8s.deploy.model.Topic;
import org.skipperlab.k8s.deploy.service.KafkaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

    private final KafkaService kafkaAdmin;

    public KafkaController(KafkaService kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @GetMapping("/topics")
    public List<Topic> allTopics() throws ExecutionException, InterruptedException {
        return kafkaAdmin.allTopics();
    }

    @GetMapping("/topics/{name}")
    public Topic getTopic(@PathVariable String name) throws ExecutionException, InterruptedException, JsonProcessingException {
        return kafkaAdmin.getTopicAndConfig(name);
    }
}