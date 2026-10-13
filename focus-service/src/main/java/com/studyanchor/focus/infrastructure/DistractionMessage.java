package com.studyanchor.focus.infrastructure;

import com.studyanchor.focus.domain.DistractionLogged;

/** Implements FR-12 AC1. What the spike stream reads from {@code focus.distractions}: it only counts, so only the type. */
record DistractionMessage(String eventType) {

    boolean isLogged() {
        return DistractionLogged.TYPE.equals(eventType);
    }
}
