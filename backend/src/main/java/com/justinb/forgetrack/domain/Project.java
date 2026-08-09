package com.justinb.forgetrack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "projects", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_organization_key", columnNames = {"organization_id", "project_key"})
})
public class Project extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "project_key", nullable = false, length = 10)
    private String key;

    @Column(length = 500)
    private String description;

    @Column(length = 200)
    private String githubRepository;

    @Column(nullable = false)
    private int nextIssueNumber = 1;

    @Version
    private long version;

    protected Project() {
    }

    public Project(Organization organization, String name, String key, String description, String githubRepository) {
        this.organization = organization;
        this.name = name;
        this.key = key.toUpperCase();
        this.description = description;
        this.githubRepository = githubRepository;
    }

    public int reserveIssueNumber() {
        return nextIssueNumber++;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public String getGithubRepository() {
        return githubRepository;
    }
}

