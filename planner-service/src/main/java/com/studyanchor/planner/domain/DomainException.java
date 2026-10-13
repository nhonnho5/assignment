package com.studyanchor.planner.domain;

/** A business rule was broken. Mapped to HTTP 400 by the presentation layer. */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    /** Rejects a missing request value with a 400 instead of a NullPointerException (500). */
    public static <T> T required(T value, String field) {
        if (value == null) {
            throw new DomainException(field + " is required");
        }
        return value;
    }
}
