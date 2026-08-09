package com.justinb.forgetrack.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "memberships", uniqueConstraints = {
        @UniqueConstraint(name = "uk_membership_organization_user", columnNames = {"organization_id", "user_id"})
})
public class Membership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    protected Membership() {
    }

    public Membership(Organization organization, UserAccount user, MemberRole role) {
        this.organization = organization;
        this.user = user;
        this.role = role;
    }

    public Organization getOrganization() {
        return organization;
    }

    public UserAccount getUser() {
        return user;
    }

    public MemberRole getRole() {
        return role;
    }
}

