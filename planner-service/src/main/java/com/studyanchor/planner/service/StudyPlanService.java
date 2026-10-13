package com.studyanchor.planner.service;

import com.studyanchor.planner.domain.ConflictException;
import com.studyanchor.planner.domain.Course;
import com.studyanchor.planner.domain.CourseRepository;
import com.studyanchor.planner.domain.DomainException;
import com.studyanchor.planner.domain.NotFoundException;
import com.studyanchor.planner.domain.PlanningAgent;
import com.studyanchor.planner.domain.StudyPlan;
import com.studyanchor.planner.domain.StudyPlanRepository;
import com.studyanchor.planner.domain.WeeklyAvailability;
import com.studyanchor.planner.domain.WeeklyAvailabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Implements FR-04 and FR-12 AC3, AC4, AC12. Application service for weekly study plans: checks what the agent
 * needs, lets the agent fill the plan, saves it, and tracks blocks from focus sessions. If the agent fails, the
 * exception rolls the request back, so an existing plan stays as it was (AC13).
 */
@Service
public class StudyPlanService {

    private final StudyPlanRepository plans;
    private final CourseRepository courses;
    private final WeeklyAvailabilityRepository availabilities;
    private final PlanningAgent agent;
    private final ReminderService reminders;
    private final Clock clock;

    public StudyPlanService(StudyPlanRepository plans, CourseRepository courses,
                            WeeklyAvailabilityRepository availabilities, PlanningAgent agent,
                            ReminderService reminders, Clock clock) {
        this.plans = plans;
        this.courses = courses;
        this.availabilities = availabilities;
        this.agent = agent;
        this.reminders = reminders;
        this.clock = clock;
    }

    /**
     * AC1, AC8, AC9: a Monday of a week not yet over; availability and topics before the LLM is called; an
     * existing plan for the week is replaced only when the student confirms with {@code replace}.
     */
    @Transactional
    public StudyPlan generate(String userId, LocalDate weekStart, boolean replace) {
        StudyPlan plan = StudyPlan.create(userId, weekStart, clock.instant());
        WeeklyAvailability availability = availabilities.findByUserId(userId)
                .filter(a -> !a.slots().isEmpty())
                .orElseThrow(() -> new DomainException("Add availability slots (POST /availability/slots) first"));
        List<Course> studentCourses = courses.findByUserId(userId);
        if (studentCourses.stream().allMatch(c -> c.topics().isEmpty())) {
            throw new DomainException("Add a course with at least one topic first");
        }
        StudyPlan existing = plans.findByUserIdAndWeekStart(userId, weekStart).orElse(null);
        if (existing != null) {
            if (!replace) {
                throw new ConflictException("You already have a plan for the week of " + weekStart + " with "
                        + existing.blocks().size() + " block(s). To replace it, send the request again with"
                        + " \"replace\": true.");
            }
            existing.startOver();
            plan = existing;
        }
        plan.summarise(agent.plan(plan, studentCourses, availability));
        return plans.save(plan);
    }

    @Transactional(readOnly = true)
    public StudyPlan get(String userId, LocalDate weekStart) {
        return plans.findByUserIdAndWeekStart(userId, weekStart)
                .orElseThrow(() -> new NotFoundException("No plan for the week of " + weekStart));
    }

    /** FR-12 AC12: a session started for a block, so the block will not count as missed. */
    @Transactional
    public void onFocusSessionStarted(String userId, UUID blockId) {
        updateBlock(userId, blockId, plan -> plan.sessionStarted(blockId));
    }

    /** FR-12 AC3: a session for a block ended, so the block is completed. */
    @Transactional
    public void onFocusSessionEnded(String userId, UUID blockId) {
        updateBlock(userId, blockId, plan -> plan.sessionEnded(blockId));
    }

    /** FR-12 AC4: marks blocks that ended without a session as missed, with one reminder each. */
    @Transactional
    public void markMissedBlocks() {
        Instant now = clock.instant();
        for (StudyPlan plan : plans.findWithUnstartedBlocksEndedBy(now)) {
            plan.markMissed(now).forEach(block -> reminders.onMissedBlock(plan.userId(), block));
            plans.save(plan);
        }
    }

    /** An unknown block, or another student's block, is ignored (FR-12 AC12). */
    private void updateBlock(String userId, UUID blockId, Consumer<StudyPlan> change) {
        plans.findByBlockId(blockId).filter(p -> p.userId().equals(userId)).ifPresent(plan -> {
            change.accept(plan);
            plans.save(plan);
        });
    }
}
