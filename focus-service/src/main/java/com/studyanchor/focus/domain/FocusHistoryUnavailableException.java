package com.studyanchor.focus.domain;

/**
 * Implements FR-10. Thrown by a {@link FocusHistory} when the history cannot be read right now
 * (e.g. the stream's state store is still starting or rebalancing). Not a rule violation, so it
 * is not a {@link DomainException}; the API maps it to 503.
 */
public class FocusHistoryUnavailableException extends RuntimeException {

    public FocusHistoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
