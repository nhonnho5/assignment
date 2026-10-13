package com.studyanchor.planner.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implements FR-04 AC2. Adapter: reads the student's focus progress from the Focus service over REST
 * ({@code GET /progress/{userId}}, FR-10 AC8). Any failure is turned into "unavailable" by the tool.
 */
@Component
class FocusProgressClient implements PlanningTools.FocusStats {

    private final RestClient rest;

    FocusProgressClient(RestClient.Builder builder, @Value("${focus.base-url}") String baseUrl) {
        this.rest = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Progress progressOf(String userId) {
        return rest.get().uri("/progress/{userId}", userId).retrieve().body(Progress.class);
    }
}
