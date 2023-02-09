package org.skipperlab.k8s.deploy.repository;

import org.skipperlab.k8s.deploy.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(collectionResourceRel = "workloads", path = "workloads")
public interface WorkloadRepository extends JpaRepository<Workspace, Long> {
}
