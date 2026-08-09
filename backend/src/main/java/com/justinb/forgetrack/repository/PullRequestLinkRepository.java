package com.justinb.forgetrack.repository;

import com.justinb.forgetrack.domain.PullRequestLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PullRequestLinkRepository extends JpaRepository<PullRequestLink, UUID> {
    List<PullRequestLink> findByIssueIdOrderByCreatedAtDesc(UUID issueId);
}
