package com.justinb.forgetrack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "pull_request_links", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pull_request_issue_repo_number", columnNames = {"issue_id", "repository", "pull_request_number"})
})
public class PullRequestLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Column(nullable = false, length = 200)
    private String repository;

    @Column(name = "pull_request_number", nullable = false)
    private int number;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 30)
    private String state;

    protected PullRequestLink() {
    }

    public PullRequestLink(Issue issue, String repository, int number, String title, String url, String state) {
        this.issue = issue;
        this.repository = repository;
        this.number = number;
        this.title = title;
        this.url = url;
        this.state = state;
    }

    public String getRepository() {
        return repository;
    }

    public int getNumber() {
        return number;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getState() {
        return state;
    }
}
