package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.service.StudyPlanService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Implements FR-12 AC4. A Spring {@code @Scheduled} job that looks for missed time blocks every minute. */
@Configuration
@EnableScheduling
class MissedBlockScheduler {

    private final StudyPlanService plans;

    MissedBlockScheduler(StudyPlanService plans) {
        this.plans = plans;
    }

    @Scheduled(fixedRate = 60_000)
    void markMissedBlocks() {
        plans.markMissedBlocks();
    }
}
