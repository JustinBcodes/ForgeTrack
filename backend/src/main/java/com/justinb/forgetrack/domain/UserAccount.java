package com.justinb.forgetrack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_accounts")
public class UserAccount extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    protected UserAccount() {
    }

    public UserAccount(String displayName, String email) {
        this.displayName = displayName;
        this.email = email.toLowerCase();
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }
}

