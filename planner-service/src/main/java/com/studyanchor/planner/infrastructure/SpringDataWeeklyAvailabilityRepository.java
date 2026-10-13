package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.WeeklyAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data provides save and findById; the ID of a student's availability is their userId. */
interface SpringDataWeeklyAvailabilityRepository extends JpaRepository<WeeklyAvailability, String> {
}
