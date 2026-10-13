package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data generates the SQL for these methods from their names ({@code CodeValue} = {@code code.value}). */
interface SpringDataCourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findByIdAndUserId(UUID id, String userId);

    List<Course> findByUserIdOrderByCodeValue(String userId);

    boolean existsByUserIdAndCodeValue(String userId, String code);

    /** {@code TopicsId} = a topic in {@code topics} with this {@code id}. */
    Optional<Course> findByTopicsId(UUID topicId);
}
