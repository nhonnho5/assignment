package com.studyanchor.focus.domain;

/** A business rule was broken. Mapped to HTTP 400 by the presentation layer. */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
