package com.studyanchor.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Implements FR-08 AC3. Aggregate root (one per student, identified by userId): whether others may see
 * this student's presence. Off by default; a student without a saved setting is treated as not sharing.
 */
@Entity
@Table(name = "sharing_setting")
public class SharingSetting {

    @Id
    private String userId;

    @Column(nullable = false)
    private boolean sharing;

    /** For JPA only. */
    protected SharingSetting() {
    }

    /** FR-08 AC3: sharing is off until the student turns it on. */
    public static SharingSetting defaultFor(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new DomainException("userId is required");
        }
        SharingSetting setting = new SharingSetting();
        setting.userId = userId;
        setting.sharing = false;
        return setting;
    }

    /** Accepts a possibly missing value from a request and rejects it if absent. */
    public void change(Boolean sharing) {
        if (sharing == null) {
            throw new DomainException("sharing (true or false) is required");
        }
        this.sharing = sharing;
    }

    public String userId() {
        return userId;
    }

    public boolean isSharing() {
        return sharing;
    }
}
