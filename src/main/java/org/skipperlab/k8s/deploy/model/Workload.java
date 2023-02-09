package org.skipperlab.k8s.deploy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Workload {
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne
    @JoinColumn(name="palette_id")
    private Palette palette;
    private KafkaType kafkaType;
    @Column(unique=true, nullable = false)
    private String name;
    @ManyToOne
    @JoinColumn(name="workspace_id")
    private Workspace workspace;
    private StatusType status;
    @Column(columnDefinition = "TEXT")
    private String config;

    public String getNameSpace() {
        return null;
    }
}
