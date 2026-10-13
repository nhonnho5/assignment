package com.studyanchor.planner.service;

import com.studyanchor.planner.domain.ConflictException;
import com.studyanchor.planner.domain.Course;
import com.studyanchor.planner.domain.CourseRepository;
import com.studyanchor.planner.domain.Deadline;
import com.studyanchor.planner.domain.EventPublisher;
import com.studyanchor.planner.domain.NotFoundException;
import com.studyanchor.planner.domain.Topic;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Implements FR-01. Application service for courses, topics and deadlines: loads the student's course,
 * calls the aggregate, saves it, then publishes the events it registered.
 */
@Service
public class CourseService {

    private final CourseRepository repository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public CourseService(CourseRepository repository, EventPublisher eventPublisher, Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /** FR-01 AC6: the same course code twice for one student → 409. */
    @Transactional
    public Course create(String userId, String code, String name) {
        Course course = Course.create(userId, code, name);
        if (repository.existsByUserIdAndCode(course.userId(), course.code())) {
            throw new ConflictException("You already have course " + course.code().value());
        }
        return repository.save(course);
    }

    @Transactional(readOnly = true)
    public List<Course> list(String userId) {
        return repository.findByUserId(userId);
    }

    /** FR-01 AC5: another student's course → 404. */
    @Transactional(readOnly = true)
    public Course get(String userId, UUID courseId) {
        return repository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new NotFoundException("Course " + courseId + " not found"));
    }

    /** FR-01 AC9: removes the course with its topics and deadlines; one TopicDeleted per topic. */
    @Transactional
    public void delete(String userId, UUID courseId) {
        Course course = get(userId, courseId);
        course.delete(clock.instant());
        repository.delete(course);
        course.pullDomainEvents().forEach(eventPublisher::publish);
    }

    @Transactional
    public Topic addTopic(String userId, UUID courseId, String name) {
        Course course = get(userId, courseId);
        Topic topic = course.addTopic(name, clock.instant());
        saveAndPublish(course);
        return topic;
    }

    @Transactional
    public void deleteTopic(String userId, UUID courseId, UUID topicId) {
        Course course = get(userId, courseId);
        course.deleteTopic(topicId, clock.instant());
        saveAndPublish(course);
    }

    @Transactional
    public Deadline addDeadline(String userId, UUID courseId, String title, LocalDateTime dueAt, Integer weight,
                                Collection<UUID> topicIds) {
        Course course = get(userId, courseId);
        Deadline deadline = course.addDeadline(title, dueAt, weight, topicIds, clock.instant());
        saveAndPublish(course);
        return deadline;
    }

    @Transactional
    public void deleteDeadline(String userId, UUID courseId, UUID deadlineId) {
        Course course = get(userId, courseId);
        course.deleteDeadline(deadlineId);
        saveAndPublish(course);
    }

    private void saveAndPublish(Course course) {
        repository.save(course);
        course.pullDomainEvents().forEach(eventPublisher::publish);
    }
}
