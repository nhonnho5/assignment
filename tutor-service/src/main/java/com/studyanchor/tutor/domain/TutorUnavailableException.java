package com.studyanchor.tutor.domain;

/** Implements FR-07 AC12. The LLM failed (quota, network, missing key). Mapped to HTTP 503; nothing is saved. */
public class TutorUnavailableException extends RuntimeException {

    public TutorUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
