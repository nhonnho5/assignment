package com.studyanchor.tutor.domain;

/** Implements FR-02 AC5. The one shared check for text: required, trimmed, length-limited. */
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
}
