package org.skipperlab.k8s.deploy.repository;

import org.skipperlab.k8s.deploy.model.Command;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(collectionResourceRel = "commands", path = "commands")
public interface CommandRepository extends JpaRepository<Command, Long> {
    Command findByName(@Param("name") String name);
    List<Command> findByPaletteId(@Param("paletteId") Long paletteId);
}
