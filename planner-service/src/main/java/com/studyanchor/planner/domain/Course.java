package com.studyanchor.planner.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.studyanchor.planner.domain.DomainException.required;

/**
 * Implements FR-01. Aggregate root: one student's course with its {@link Topic}s and {@link Deadline}s (AC5).
 * Topics and deadlines change only through this class, which registers {@link TopicCreated} and
 * {@link TopicDeleted} for the application service to publish after saving.
 */
@Entity
@Table(name = "course")
public class Course {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "code", nullable = false))
    private CourseCode code;

    @Column(nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Set<Topic> topics = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Set<Deadline> deadlines = new HashSet<>();

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /** For JPA only. */
    protected Course() {
    }

    /** FR-01 AC1, AC6. Uniqueness of the code per student needs all courses, so the service checks it. */
    public static Course create(String userId, String code, String name) {
        Course course = new Course();
        course.id = UUID.randomUUID();
        course.userId = Text.userId(userId);
        course.code = new CourseCode(code);
        course.name = Text.of(name, "Course name", 100);
        return course;
    }

    /** FR-01 AC1, AC4, AC6: topic names are unique in the course, ignoring case (409). */
    public Topic addTopic(String name, Instant now) {
        Topic topic = new Topic(name);
        if (topics.stream().anyMatch(t -> t.name().equalsIgnoreCase(topic.name()))) {
            throw new ConflictException("Course " + code.value() + " already has a topic named " + topic.name());
        }
        topics.add(topic);
        domainEvents.add(TopicCreated.of(this, topic, now));
        return topic;
    }

    /** FR-01 AC2, AC3, AC7, AC8: due in the future (Sydney time), topics of this course, total weight ≤ 100 %. */
    public Deadline addDeadline(String title, LocalDateTime dueAt, Integer weight, Collection<UUID> topicIds,
                                Instant now) {
        Instant due = StudyCalendar.toInstant(required(dueAt, "dueAt"));
        if (!due.isAfter(now)) {
            throw new DomainException("dueAt must be in the future (Australia/Sydney time)");
        }
        if (topicIds == null || topicIds.isEmpty() || !topicIds.stream().allMatch(this::hasTopic)) {
            throw new DomainException("A deadline needs one or more topic IDs of course " + code.value());
        }
        Weight w = new Weight(required(weight, "weight"));
        int total = deadlines.stream().mapToInt(d -> d.weight().value()).sum() + w.value();
        if (total > 100) {
            throw new ConflictException("Total deadline weight of " + code.value() + " would be " + total + " %");
        }
        Deadline deadline = new Deadline(title, due, w, Set.copyOf(topicIds));
        deadlines.add(deadline);
        return deadline;
    }

    /** FR-01 AC9: a topic still used by a deadline cannot be deleted (409). */
    public void deleteTopic(UUID topicId, Instant now) {
        Topic topic = topic(topicId);
        if (deadlines.stream().anyMatch(d -> d.topicIds().contains(topicId))) {
            throw new ConflictException("Topic " + topic.name() + " is used by a deadline; delete the deadline first");
        }
        topics.remove(topic);
        domainEvents.add(TopicDeleted.of(this, topic, now));
    }

    /** FR-02 AC8: Tutor reported material for one of this course's topics; repeats are counted once. */
    public void recordMaterial(UUID topicId, UUID materialId) {
        topic(topicId).recordMaterial(materialId);
    }

    /** FR-07 AC14: Tutor recorded an understanding level for one of this course's topics; the latest one wins. */
    public void recordUnderstanding(UUID topicId, int level, Instant recordedAt) {
        topic(topicId).recordUnderstanding(level, recordedAt);
    }

    public void deleteDeadline(UUID deadlineId) {
        if (!deadlines.removeIf(d -> d.id().equals(deadlineId))) {
            throw new NotFoundException("Deadline " + deadlineId + " not found");
        }
    }

    /** FR-01 AC9: called before the course is removed, so Tutor hears that every topic is gone. */
    public void delete(Instant now) {
        topics.forEach(t -> domainEvents.add(TopicDeleted.of(this, t, now)));
    }

    /** Returns the events registered since the last call and forgets them, so each is published once. */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private Topic topic(UUID topicId) {
        return topics.stream().filter(t -> t.id().equals(topicId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Topic " + topicId + " not found"));
    }

    private boolean hasTopic(UUID topicId) {
        return topics.stream().anyMatch(t -> t.id().equals(topicId));
    }

    public UUID id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public CourseCode code() {
        return code;
    }

    public String name() {
        return name;
    }

    public List<Topic> topics() {
        return List.copyOf(topics);
    }

    public List<Deadline> deadlines() {
        return List.copyOf(deadlines);
    }
}
