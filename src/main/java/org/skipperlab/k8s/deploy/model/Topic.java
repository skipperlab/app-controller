package org.skipperlab.k8s.deploy.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String topicId;
    @Column(unique=true, nullable = false)
    private String name;
    private Boolean isInternal;
    private Integer partitions;
    private Integer replicas;
    private StatusType status;
    @Column(columnDefinition = "TEXT")
    private String config;
}
