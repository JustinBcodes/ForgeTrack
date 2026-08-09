package com.justinb.forgetrack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "issue_activities")
public class IssueActivity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private UserAccount actor;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false, length = 500)
    private String details;

    protected IssueActivity() {
    }

    public IssueActivity(Issue issue, UserAccount actor, String action, String details) {
        this.issue = issue;
        this.actor = actor;
        this.action = action;
        this.details = details;
    }

    public UserAccount getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }
}

