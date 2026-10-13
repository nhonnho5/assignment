package com.studyanchor.social.domain;

/** The request clashes with the current state (e.g. joining a room twice). Mapped to HTTP 409. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
