package com.studyanchor.planner.domain;

/** Implements FR-04 AC13. The LLM failed (quota, network, missing key). Mapped to HTTP 503; nothing is saved. */
public class AgentUnavailableException extends RuntimeException {

    public AgentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
