package org.skipperlab.k8s.deploy.repository;

import org.skipperlab.k8s.deploy.model.Palette;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(collectionResourceRel = "palettes", path = "palettes")
public interface PaletteRepository extends JpaRepository<Palette, Long> {
    Palette findByName(@Param("name") String name);
}
