package com.studyanchor.social.domain;

/**
 * Implements FR-09. Thrown by a {@link PresenceBoard} when presence cannot be read right now (e.g. the
 * stream's state store is still starting or rebalancing). Not a rule violation, so it is not a
 * {@link DomainException}; the API maps it to 503.
 */
public class PresenceUnavailableException extends RuntimeException {

    public PresenceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
