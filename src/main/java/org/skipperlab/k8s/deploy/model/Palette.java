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
public class Palette {
    @Id
    @GeneratedValue
    private Long id;
    @Column(unique=true, nullable = false)
    private String name;
    private KafkaType kafkaType;
    private String yamlPath;
    @Column(columnDefinition = "TEXT")
    private String configTemplate;
}
