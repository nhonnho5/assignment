package com.studyanchor.planner.domain;

/** The request clashes with the current state (e.g. a duplicate course code). Mapped to HTTP 409. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
