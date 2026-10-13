package com.studyanchor.social.domain;

/** The requested thing does not exist (or is hidden from this user). Mapped to HTTP 404. */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }
}
