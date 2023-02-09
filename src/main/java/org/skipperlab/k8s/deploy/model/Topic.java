package org.skipperlab.k8s.deploy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Topic {
    @Id
    @GeneratedValue
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
