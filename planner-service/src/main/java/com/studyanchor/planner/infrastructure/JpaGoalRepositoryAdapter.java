package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.Goal;
import com.studyanchor.planner.domain.GoalRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements FR-11. Adapter: fulfils the domain repository interface using Spring Data JPA and H2. */
@Repository
class JpaGoalRepositoryAdapter implements GoalRepository {

    private final SpringDataGoalRepository jpa;

    JpaGoalRepositoryAdapter(SpringDataGoalRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Goal save(Goal goal) {
        return jpa.save(goal);
    }

    @Override
    public Optional<Goal> findByIdAndUserId(UUID id, String userId) {
        return jpa.findByIdAndUserId(id, userId);
    }

    @Override
    public List<Goal> findByUserId(String userId) {
        return jpa.findByUserIdOrderByCreatedAt(userId);
    }

    @Override
    public void delete(Goal goal) {
        jpa.delete(goal);
    }
}
