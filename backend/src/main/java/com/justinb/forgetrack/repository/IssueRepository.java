package com.justinb.forgetrack.repository;

import com.justinb.forgetrack.domain.Issue;
import com.justinb.forgetrack.domain.IssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {

    long countByProjectId(UUID projectId);

    long countByProjectIdAndStatus(UUID projectId, IssueStatus status);

    @EntityGraph(attributePaths = {"project", "assignee", "reporter"})
    List<Issue> findTop5ByProjectIdOrderByUpdatedAtDesc(UUID projectId);

    @Override
    @EntityGraph(attributePaths = {"project", "assignee", "reporter"})
    Page<Issue> findAll(org.springframework.data.jpa.domain.Specification<Issue> specification, Pageable pageable);
}

