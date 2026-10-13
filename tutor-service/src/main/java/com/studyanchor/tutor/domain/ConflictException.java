package com.studyanchor.tutor.domain;

/** The request clashes with the current state (e.g. ending a session twice). Mapped to HTTP 409. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
