package com.studyanchor.planner.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements FR-01, FR-02 AC8. Repository for the {@link Course} aggregate; implemented in infrastructure. */
public interface CourseRepository {

    Course save(Course course);

    /** Finds a course only if it belongs to the given user, so other users' courses look like "not found". */
    Optional<Course> findByIdAndUserId(UUID id, String userId);

    List<Course> findByUserId(String userId);

    /** FR-02 AC8: the course that holds this topic, if any. */
    Optional<Course> findByTopicId(UUID topicId);

    boolean existsByUserIdAndCode(String userId, CourseCode code);

    void delete(Course course);
}
