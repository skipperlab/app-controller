package org.skipperlab.k8s.deploy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Palette {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(unique=true, nullable = false)
    private String name;
    private KafkaType kafkaType;
    private String yamlPath;
    @Column(columnDefinition = "TEXT")
    private String configTemplate;
}
