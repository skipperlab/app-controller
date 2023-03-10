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
public class Workload {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    @JoinColumn(name="palette_id")
    private Palette palette;
    @Column(unique=true, nullable = false)
    private String name;
    private String inputTopics;
    private String outputTopics;
    @ManyToOne
    @JoinColumn(name="workspace_id")
    private Workspace workspace;
    private StatusType status;
    @Column(columnDefinition = "TEXT")
    private String config;

    public String getNameSpace() {
        if(workspace != null) return workspace.getName();
        else return this.getName();
    }
}
