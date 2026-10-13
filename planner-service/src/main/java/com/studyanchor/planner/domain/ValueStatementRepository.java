package com.studyanchor.planner.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements FR-11. Repository for the {@link ValueStatement} aggregate; implemented in infrastructure. */
public interface ValueStatementRepository {

    ValueStatement save(ValueStatement value);

    /** Finds a value statement only if it belongs to the given user (otherwise "not found"). */
    Optional<ValueStatement> findByIdAndUserId(UUID id, String userId);

    /** Oldest first. */
    List<ValueStatement> findByUserId(String userId);

    void delete(ValueStatement value);
}
