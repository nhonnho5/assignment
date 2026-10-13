package com.studyanchor.focus.domain;

/** The requested thing does not exist (or belongs to another user). Mapped to HTTP 404. */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }
}
