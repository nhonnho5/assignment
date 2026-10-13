package com.studyanchor.planner.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements FR-11. Repository for the {@link Goal} aggregate; implemented in infrastructure. */
public interface GoalRepository {

    Goal save(Goal goal);

    /** Finds a goal only if it belongs to the given user, so other users' goals look like "not found". */
    Optional<Goal> findByIdAndUserId(UUID id, String userId);

    /** Oldest first. */
    List<Goal> findByUserId(String userId);

    void delete(Goal goal);
}
