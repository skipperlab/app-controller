package org.skipperlab.k8s.deploy.repository;

import org.skipperlab.k8s.deploy.model.Workload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RepositoryRestResource(collectionResourceRel = "workloads", path = "workloads")
public interface WorkloadRepository extends JpaRepository<Workload, Long> {
    Workload findByName(@Param("name") String name);
    List<Workload> findByWorkspaceId(@Param("workspaceId") Long workspaceId);
    List<Workload> findByPaletteId(@Param("paletteId") Long paletteId);
}
