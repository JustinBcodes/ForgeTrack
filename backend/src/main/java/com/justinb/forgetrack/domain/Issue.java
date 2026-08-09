package com.justinb.forgetrack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "issues", uniqueConstraints = {
        @UniqueConstraint(name = "uk_issue_project_number", columnNames = {"project_id", "issue_number"})
})
public class Issue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "issue_number", nullable = false)
    private int number;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IssueType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IssueStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IssuePriority priority;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserAccount reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private UserAccount assignee;

    protected Issue() {
    }

    public Issue(Project project, int number, String title, String description, IssueType type,
                 IssuePriority priority, UserAccount reporter, UserAccount assignee) {
        this.project = project;
        this.number = number;
        this.title = title;
        this.description = description;
        this.type = type;
        this.status = IssueStatus.TODO;
        this.priority = priority;
        this.reporter = reporter;
        this.assignee = assignee;
    }

    public void update(String title, String description, IssueType type, IssueStatus status,
                       IssuePriority priority, UserAccount assignee) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.status = status;
        this.priority = priority;
        this.assignee = assignee;
    }

    public Project getProject() {
        return project;
    }

    public int getNumber() {
        return number;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public IssueType getType() {
        return type;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public IssuePriority getPriority() {
        return priority;
    }

    public UserAccount getReporter() {
        return reporter;
    }

    public UserAccount getAssignee() {
        return assignee;
    }
}

