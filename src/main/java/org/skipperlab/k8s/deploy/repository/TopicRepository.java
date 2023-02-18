package org.skipperlab.k8s.deploy.repository;

import org.skipperlab.k8s.deploy.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

@Repository
@RepositoryRestResource(collectionResourceRel = "topics", path = "topics")
public interface TopicRepository extends JpaRepository<Topic, Long> {
    Topic findByName(@Param("name") String name);
}
