package com.justinb.forgetrack.repository;

import com.justinb.forgetrack.domain.IssueComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IssueCommentRepository extends JpaRepository<IssueComment, UUID> {

    @EntityGraph(attributePaths = "author")
    List<IssueComment> findByIssueIdOrderByCreatedAtAsc(UUID issueId);
}

