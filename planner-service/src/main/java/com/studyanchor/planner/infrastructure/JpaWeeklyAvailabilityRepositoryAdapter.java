package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.WeeklyAvailability;
import com.studyanchor.planner.domain.WeeklyAvailabilityRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Implements FR-03. Adapter: fulfils the domain repository interface using Spring Data JPA and H2. */
@Repository
class JpaWeeklyAvailabilityRepositoryAdapter implements WeeklyAvailabilityRepository {

    private final SpringDataWeeklyAvailabilityRepository jpa;

    JpaWeeklyAvailabilityRepositoryAdapter(SpringDataWeeklyAvailabilityRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public WeeklyAvailability save(WeeklyAvailability availability) {
        return jpa.save(availability);
    }

    @Override
    public Optional<WeeklyAvailability> findByUserId(String userId) {
        return jpa.findById(userId);
    }
}
