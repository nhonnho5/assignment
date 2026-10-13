package com.studyanchor.focus.presentation;

/** Implements FR-06 AC2. Request body for {@code POST /sessions/{id}/distractions}. Validation happens in the domain. */
public record LogDistractionRequest(String type, String site, String note) {
}
