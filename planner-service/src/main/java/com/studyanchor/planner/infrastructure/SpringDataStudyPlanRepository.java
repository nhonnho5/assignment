package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.StudyPlan;
import com.studyanchor.planner.domain.TimeBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data generates the SQL for these methods from their names, or from the JPQL query. */
interface SpringDataStudyPlanRepository extends JpaRepository<StudyPlan, UUID> {

    Optional<StudyPlan> findByUserIdAndWeekStart(String userId, LocalDate weekStart);

    /** {@code BlocksId} = a block in {@code blocks} with this {@code id}. */
    Optional<StudyPlan> findByBlocksId(UUID blockId);

    @Query("select distinct p from StudyPlan p join p.blocks b where b.status = :status and b.endsAt <= :now")
    List<StudyPlan> findWithBlocksEndedBy(@Param("status") TimeBlock.Status status, @Param("now") Instant now);
}
