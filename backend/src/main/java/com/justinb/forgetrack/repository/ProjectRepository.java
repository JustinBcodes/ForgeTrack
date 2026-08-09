package com.justinb.forgetrack.repository;

import com.justinb.forgetrack.domain.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @EntityGraph(attributePaths = "organization")
    List<Project> findAllByOrderByName();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Project> findForUpdateById(UUID id);
}

