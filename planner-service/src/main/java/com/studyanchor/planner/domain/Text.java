package com.studyanchor.planner.domain;

/** Implements FR-01 AC6, FR-11 AC3, AC5. The one shared check for typed text: required, trimmed, length-limited. */
public final class Text {

    private Text() {
    }

    /** Returns the trimmed text, or throws if it is missing, blank or longer than {@code max} characters. */
    public static String of(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new DomainException(field + " must be at most " + max + " characters, was " + trimmed.length());
        }
        return trimmed;
    }

    /** The owner from the {@code X-User-Id} header: any non-blank ID (there is no user registry). */
    public static String userId(String value) {
        return of(value, "userId", Integer.MAX_VALUE);
    }
}
