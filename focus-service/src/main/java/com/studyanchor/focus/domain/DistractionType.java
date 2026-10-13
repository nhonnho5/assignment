package com.studyanchor.focus.domain;

import java.util.Arrays;
import java.util.Locale;

/** Implements FR-06 AC2, AC6. What kind of interruption a distraction was. */
public enum DistractionType {
    PHONE, SOCIAL_MEDIA, OTHER_SITE, THOUGHT, OTHER;

    /** Case-insensitive ({@code phone} = {@code PHONE}); missing or unknown → 400. */
    public static DistractionType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException("type is required");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DomainException("type must be one of " + Arrays.toString(values()) + ", was " + value.trim());
        }
    }
}
