package com.studyanchor.focus.presentation;

import java.util.List;
import java.util.UUID;

/** Implements FR-05 AC1. Request body for {@code POST /sessions}. Validation happens in the domain. */
public record StartSessionRequest(UUID timeBlockId, UUID topicId, Integer plannedMinutes, List<String> allowedSites) {
}
