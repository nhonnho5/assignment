package com.studyanchor.tutor.domain;

/** Implements FR-07 AC10. A tutoring session moves ACTIVE → CHECKING (student pressed end) → ENDED (level recorded). */
public enum SessionStatus {
    ACTIVE, CHECKING, ENDED
}
