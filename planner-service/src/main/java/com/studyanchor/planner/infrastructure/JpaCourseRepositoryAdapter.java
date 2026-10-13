package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.Course;
import com.studyanchor.planner.domain.CourseCode;
import com.studyanchor.planner.domain.CourseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements FR-01. Adapter: fulfils the domain repository interface using Spring Data JPA and H2. */
@Repository
class JpaCourseRepositoryAdapter implements CourseRepository {

    private final SpringDataCourseRepository jpa;

    JpaCourseRepositoryAdapter(SpringDataCourseRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Course save(Course course) {
        return jpa.save(course);
    }

    @Override
    public Optional<Course> findByIdAndUserId(UUID id, String userId) {
        return jpa.findByIdAndUserId(id, userId);
    }

    @Override
    public List<Course> findByUserId(String userId) {
        return jpa.findByUserIdOrderByCodeValue(userId);
    }

    @Override
    public Optional<Course> findByTopicId(UUID topicId) {
        return jpa.findByTopicsId(topicId);
    }

    @Override
    public boolean existsByUserIdAndCode(String userId, CourseCode code) {
        return jpa.existsByUserIdAndCodeValue(userId, code.value());
    }

    @Override
    public void delete(Course course) {
        jpa.delete(course);
    }
}
