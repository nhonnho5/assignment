package com.studyanchor.focus.presentation;

import com.studyanchor.focus.domain.Distraction;
import com.studyanchor.focus.domain.DistractionType;

import java.time.Instant;

/** Implements FR-06 AC8. What the API returns for one distraction. */
public record DistractionResponse(DistractionType type, String site, String note, boolean offPolicy, Instant loggedAt) {

    static DistractionResponse from(Distraction d) {
        return new DistractionResponse(d.type(), d.site(), d.note(), d.offPolicy(), d.loggedAt());
    }
}
