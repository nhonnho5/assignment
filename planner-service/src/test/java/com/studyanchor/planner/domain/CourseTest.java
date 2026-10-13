package com.studyanchor.planner.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Domain rules for FR-01 courses, topics and deadlines. Plain Java, no Spring. */
class CourseTest {

    /** 14:00 in Sydney (AEDT, UTC+11). */
    private static final Instant NOW = Instant.parse("2026-10-14T03:00:00Z");
    private static final LocalDateTime NEXT_WEEK = LocalDateTime.parse("2026-10-21T23:59");

    private final Course course = Course.create("alice", "CSCI318", "Software Engineering");

    private Deadline deadline(int weight, UUID... topicIds) {
        return course.addDeadline("Assignment", NEXT_WEEK, weight, List.of(topicIds), NOW);
    }

    @Test
    void FR01_courseCodeIsTrimmedAndUpperCased() {
        assertThat(Course.create("alice", "  csci318 ", "SE").code().value()).isEqualTo("CSCI318");
        assertThatThrownBy(() -> Course.create("alice", " ", "SE")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Course.create("alice", "x".repeat(21), "SE")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Course.create("alice", "CSCI318", "x".repeat(101))).isInstanceOf(DomainException.class);
    }

    @Test
    void FR01_addingTopicRegistersTopicCreated() {
        Topic topic = course.addTopic(" Kafka ", NOW);

        assertThat(topic.name()).isEqualTo("Kafka");
        assertThat(course.pullDomainEvents()).singleElement().isInstanceOfSatisfying(TopicCreated.class, e -> {
            assertThat(e.userId()).isEqualTo("alice");
            assertThat(e.topicId()).isEqualTo(topic.id());
            assertThat(e.courseId()).isEqualTo(course.id());
            assertThat(e.courseCode()).isEqualTo("CSCI318");
            assertThat(e.topicName()).isEqualTo("Kafka");
            assertThat(e.occurredAt()).isEqualTo(NOW);
        });
        assertThat(course.pullDomainEvents()).as("each event is pulled once").isEmpty();
    }

    @Test
    void FR01_duplicateTopicNameIsConflict() {
        course.addTopic("Kafka", NOW);

        assertThatThrownBy(() -> course.addTopic(" KAFKA ", NOW)).isInstanceOf(ConflictException.class);
    }

    @Test
    void FR01_deadlineInPastIsRejected() {
        UUID topic = course.addTopic("Kafka", NOW).id();
        LocalDateTime nowInSydney = LocalDateTime.parse("2026-10-14T14:00");

        assertThatThrownBy(() -> course.addDeadline("A1", nowInSydney, 10, List.of(topic), NOW))
                .isInstanceOf(DomainException.class).isNotInstanceOf(ConflictException.class);

        Deadline d = course.addDeadline("A1", nowInSydney.plusMinutes(1), 10, List.of(topic), NOW);
        assertThat(d.dueAt()).as("stored as UTC").isEqualTo(Instant.parse("2026-10-14T03:01:00Z"));
    }

    @Test
    void FR01_deadlineWeightMustBe1To100() {
        UUID topic = course.addTopic("Kafka", NOW).id();

        assertThatThrownBy(() -> deadline(0, topic)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> deadline(101, topic)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> course.addDeadline("A1", NEXT_WEEK, null, List.of(topic), NOW))
                .isInstanceOf(DomainException.class);
        assertThat(deadline(100, topic).weight().value()).isEqualTo(100);
    }

    @Test
    void FR01_deadlineTopicsMustBelongToCourse() {
        course.addTopic("Kafka", NOW);
        UUID otherCoursesTopic = Course.create("alice", "MATH101", "Maths").addTopic("Limits", NOW).id();

        assertThatThrownBy(() -> deadline(10)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> deadline(10, otherCoursesTopic))
                .isInstanceOf(DomainException.class).isNotInstanceOf(NotFoundException.class);
    }

    @Test
    void FR01_totalWeightCannotExceed100() {
        UUID topic = course.addTopic("Kafka", NOW).id();
        deadline(60, topic);
        deadline(40, topic);

        assertThatThrownBy(() -> deadline(1, topic)).isInstanceOf(ConflictException.class);
    }

    @Test
    void FR01_cannotDeleteTopicUsedByDeadline() {
        UUID topic = course.addTopic("Kafka", NOW).id();
        Deadline d = deadline(10, topic);

        assertThatThrownBy(() -> course.deleteTopic(topic, NOW)).isInstanceOf(ConflictException.class);

        course.deleteDeadline(d.id());
        course.deleteTopic(topic, NOW);
        assertThat(course.topics()).isEmpty();
        assertThatThrownBy(() -> course.deleteDeadline(d.id())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void FR01_deletingTopicRegistersTopicDeleted() {
        UUID topic = course.addTopic("Kafka", NOW).id();
        course.pullDomainEvents();

        course.deleteTopic(topic, NOW);

        assertThat(course.pullDomainEvents()).singleElement().isInstanceOfSatisfying(TopicDeleted.class, e -> {
            assertThat(e.topicId()).isEqualTo(topic);
            assertThat(e.courseId()).isEqualTo(course.id());
            assertThat(e.userId()).isEqualTo("alice");
        });
        assertThatThrownBy(() -> course.deleteTopic(topic, NOW)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void FR01_deletingCourseRegistersTopicDeletedForEachTopic() {
        UUID kafka = course.addTopic("Kafka", NOW).id();
        UUID ddd = course.addTopic("DDD", NOW).id();
        deadline(10, kafka);
        course.pullDomainEvents();

        course.delete(NOW);

        assertThat(course.pullDomainEvents()).extracting(e -> ((TopicDeleted) e).topicId())
                .containsExactlyInAnyOrder(kafka, ddd);
    }

    @Test
    void FR02_materialCountedOncePerMaterial() {
        UUID kafka = course.addTopic("Kafka", NOW).id();
        UUID material = UUID.randomUUID();

        course.recordMaterial(kafka, material);
        course.recordMaterial(kafka, material);
        course.recordMaterial(kafka, UUID.randomUUID());

        assertThat(course.topics()).singleElement().satisfies(t -> assertThat(t.materialCount()).isEqualTo(2));
    }
}
