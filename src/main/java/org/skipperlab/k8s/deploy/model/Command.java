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
public class Command {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private CommandType commandType;
    @ManyToOne
    @JoinColumn(name="palette_id")
    private Palette palette;
    private String httpMethod;
    private String httpPath;
    private String httpBody;
}
