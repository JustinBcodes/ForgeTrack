package com.justinb.forgetrack.repository;

import com.justinb.forgetrack.domain.IssueActivity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueActivityRepository extends JpaRepository<IssueActivity, UUID> {

    @EntityGraph(attributePaths = "actor")
    List<IssueActivity> findByIssueIdOrderByCreatedAtDesc(UUID issueId);
}

