package com.studyanchor.planner.service;

import com.studyanchor.planner.domain.Goal;
import com.studyanchor.planner.domain.GoalRepository;
import com.studyanchor.planner.domain.NotFoundException;
import com.studyanchor.planner.domain.ValueStatement;
import com.studyanchor.planner.domain.ValueStatementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Implements FR-11. Application service for the student's goals and value statements (the "why"). */
@Service
public class MotivationService {

    private final GoalRepository goals;
    private final ValueStatementRepository values;
    private final Clock clock;

    public MotivationService(GoalRepository goals, ValueStatementRepository values, Clock clock) {
        this.goals = goals;
        this.values = values;
        this.clock = clock;
    }

    @Transactional
    public Goal createGoal(String userId, String title, String why, LocalDate targetDate) {
        return goals.save(Goal.create(userId, title, why, targetDate, clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<Goal> goals(String userId) {
        return goals.findByUserId(userId);
    }

    /** FR-11 AC4: achieving twice → 409 (from the aggregate). */
    @Transactional
    public Goal achieve(String userId, UUID goalId) {
        Goal goal = goal(userId, goalId);
        goal.achieve(clock.instant());
        return goals.save(goal);
    }

    @Transactional
    public void deleteGoal(String userId, UUID goalId) {
        goals.delete(goal(userId, goalId));
    }

    @Transactional
    public ValueStatement createValue(String userId, String text) {
        return values.save(ValueStatement.create(userId, text, clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<ValueStatement> values(String userId) {
        return values.findByUserId(userId);
    }

    /** FR-11 AC6: another student's value statement → 404. */
    @Transactional
    public void deleteValue(String userId, UUID valueId) {
        values.delete(values.findByIdAndUserId(valueId, userId)
                .orElseThrow(() -> new NotFoundException("Value statement " + valueId + " not found")));
    }

    /** FR-11 AC6: another student's goal → 404. */
    private Goal goal(String userId, UUID goalId) {
        return goals.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new NotFoundException("Goal " + goalId + " not found"));
    }
}
