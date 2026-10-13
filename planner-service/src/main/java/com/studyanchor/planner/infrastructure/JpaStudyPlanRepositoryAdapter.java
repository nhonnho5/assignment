package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.StudyPlan;
import com.studyanchor.planner.domain.StudyPlanRepository;
import com.studyanchor.planner.domain.TimeBlock;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements FR-04, FR-12 AC12. Adapter: fulfils the domain repository interface using Spring Data JPA and H2. */
@Repository
class JpaStudyPlanRepositoryAdapter implements StudyPlanRepository {

    private final SpringDataStudyPlanRepository jpa;

    JpaStudyPlanRepositoryAdapter(SpringDataStudyPlanRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public StudyPlan save(StudyPlan plan) {
        return jpa.save(plan);
    }

    @Override
    public Optional<StudyPlan> findByUserIdAndWeekStart(String userId, LocalDate weekStart) {
        return jpa.findByUserIdAndWeekStart(userId, weekStart);
    }

    @Override
    public Optional<StudyPlan> findByBlockId(UUID blockId) {
        return jpa.findByBlocksId(blockId);
    }

    @Override
    public List<StudyPlan> findWithUnstartedBlocksEndedBy(Instant now) {
        return jpa.findWithBlocksEndedBy(TimeBlock.Status.PLANNED, now);
    }
}
