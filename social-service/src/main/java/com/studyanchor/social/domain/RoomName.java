package com.studyanchor.social.domain;

import jakarta.persistence.Embeddable;

/** Implements FR-08 AC7. Value object: a study room's name, trimmed, 1–60 characters. */
@Embeddable
public record RoomName(String value) {

    public static final int MAX = 60;

    public RoomName {
        if (value == null || value.isBlank()) {
            throw new DomainException("Room name is required");
        }
        value = value.trim();
        if (value.length() > MAX) {
            throw new DomainException("Room name must be at most " + MAX + " characters, was " + value.length());
        }
    }
}
