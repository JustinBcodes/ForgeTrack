package com.justinb.forgetrack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "issue_comments")
public class IssueComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    protected IssueComment() {
    }

    public IssueComment(Issue issue, UserAccount author, String body) {
        this.issue = issue;
        this.author = author;
        this.body = body;
    }

    public Issue getIssue() {
        return issue;
    }

    public UserAccount getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }
}

