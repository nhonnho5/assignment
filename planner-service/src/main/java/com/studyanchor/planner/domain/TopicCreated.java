package com.studyanchor.planner.domain;

import java.time.Instant;
import java.util.UUID;

/** Implements FR-01 AC4. Published to {@code planner.topics} when a topic is created; Tutor copies the topic. */
public record TopicCreated(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String userId,
        UUID topicId,
        UUID courseId,
        String courseCode,
        String topicName) implements DomainEvent {

    static TopicCreated of(Course course, Topic topic, Instant now) {
        return new TopicCreated(UUID.randomUUID(), "TopicCreated", now, course.userId(),
                topic.id(), course.id(), course.code().value(), topic.name());
    }
}
