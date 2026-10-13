package com.studyanchor.focus.domain;

/** The request clashes with the current state (e.g. a second active session). Mapped to HTTP 409. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
