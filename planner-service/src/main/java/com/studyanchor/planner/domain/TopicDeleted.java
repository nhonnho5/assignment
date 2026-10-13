package com.studyanchor.planner.domain;

import java.time.Instant;
import java.util.UUID;

/** Implements FR-01 AC9. Published to {@code planner.topics} when a topic (or its course) is deleted. */
public record TopicDeleted(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String userId,
        UUID topicId,
        UUID courseId) implements DomainEvent {

    static TopicDeleted of(Course course, Topic topic, Instant now) {
        return new TopicDeleted(UUID.randomUUID(), "TopicDeleted", now, course.userId(), topic.id(), course.id());
    }
}
