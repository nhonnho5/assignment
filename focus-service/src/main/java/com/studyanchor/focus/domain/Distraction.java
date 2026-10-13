package com.studyanchor.focus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;
import java.util.Collection;

/**
 * Implements FR-06 AC2, AC3, AC6. Value object inside the {@link FocusSession} aggregate: one logged interruption.
 * It has no identity of its own because nothing outside the session refers to a single distraction.
 */
@Embeddable
public record Distraction(
        @Enumerated(EnumType.STRING) @Column(name = "distraction_type", nullable = false) DistractionType type,
        String site,
        @Column(length = 500) String note,
        boolean offPolicy,
        @Column(nullable = false) Instant loggedAt) {

    static final int SITE_MAX = 200;
    static final int NOTE_MAX = 500;

    /** A site that is not allowed (or any site when nothing is allowed) is off-policy; no site never is (AC3, AC7). */
    static Distraction log(String type, String site, String note, Collection<String> allowedSites, Instant now) {
        Site cleaned = Site.of(site);
        return new Distraction(DistractionType.parse(type),
                cleaned == null ? null : atMost(cleaned.host(), "site", SITE_MAX),
                note == null || note.isBlank() ? null : atMost(note.trim(), "note", NOTE_MAX),
                cleaned != null && !cleaned.isAllowedBy(allowedSites),
                now);
    }

    private static String atMost(String value, String field, int max) {
        if (value.length() > max) {
            throw new DomainException(field + " must be at most " + max + " characters, was " + value.length());
        }
        return value;
    }
}
