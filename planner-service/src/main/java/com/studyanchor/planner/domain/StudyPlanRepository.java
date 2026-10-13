package com.studyanchor.planner.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements FR-04, FR-12 AC12. Repository for the {@link StudyPlan} aggregate; implemented in infrastructure. */
public interface StudyPlanRepository {

    StudyPlan save(StudyPlan plan);

    /** AC8: one plan per student per week. */
    Optional<StudyPlan> findByUserIdAndWeekStart(String userId, LocalDate weekStart);

    /** FR-12 AC12: the plan that holds this time block, if any. */
    Optional<StudyPlan> findByBlockId(UUID blockId);

    /** FR-12 AC4: plans with a block that ended by {@code now} and is still PLANNED (no session started). */
    List<StudyPlan> findWithUnstartedBlocksEndedBy(Instant now);
}
