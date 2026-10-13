package com.studyanchor.planner.domain;

import java.util.Optional;

/** Implements FR-03. Repository for the {@link WeeklyAvailability} aggregate; implemented in infrastructure. */
public interface WeeklyAvailabilityRepository {

    WeeklyAvailability save(WeeklyAvailability availability);

    Optional<WeeklyAvailability> findByUserId(String userId);
}
