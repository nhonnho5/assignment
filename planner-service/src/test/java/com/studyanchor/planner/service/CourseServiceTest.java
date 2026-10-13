package com.studyanchor.planner.service;

import com.studyanchor.planner.domain.ConflictException;
import com.studyanchor.planner.domain.Course;
import com.studyanchor.planner.domain.CourseCode;
import com.studyanchor.planner.domain.CourseRepository;
import com.studyanchor.planner.domain.DomainEvent;
import com.studyanchor.planner.domain.NotFoundException;
import com.studyanchor.planner.domain.TopicCreated;
import com.studyanchor.planner.domain.TopicDeleted;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Application-service rules for FR-01, using an in-memory fake (no Spring, no Kafka). */
class CourseServiceTest {

    /** Keeps courses in a map instead of a database. */
    static class InMemoryCourses implements CourseRepository {

        final Map<UUID, Course> byId = new HashMap<>();

        @Override
        public Course save(Course course) {
            byId.put(course.id(), course);
            return course;
        }

        @Override
        public Optional<Course> findByIdAndUserId(UUID id, String userId) {
            return Optional.ofNullable(byId.get(id)).filter(c -> c.userId().equals(userId));
        }

        @Override
        public List<Course> findByUserId(String userId) {
            return byId.values().stream().filter(c -> c.userId().equals(userId)).toList();
        }

        @Override
        public Optional<Course> findByTopicId(UUID topicId) {
            return byId.values().stream().filter(c -> c.topics().stream().anyMatch(t -> t.id().equals(topicId)))
                    .findFirst();
        }

        @Override
        public boolean existsByUserIdAndCode(String userId, CourseCode code) {
            return findByUserId(userId).stream().anyMatch(c -> c.code().equals(code));
        }

        @Override
        public void delete(Course course) {
            byId.remove(course.id());
        }
    }

    private final InMemoryCourses courses = new InMemoryCourses();
    private final List<DomainEvent> published = new ArrayList<>();
    private final List<Boolean> courseStoredWhenPublished = new ArrayList<>();

    private final CourseService service = new CourseService(courses, event -> {
        published.add(event);
        UUID courseId = event instanceof TopicCreated e ? e.courseId() : ((TopicDeleted) event).courseId();
        courseStoredWhenPublished.add(courses.byId.containsKey(courseId));
    }, Clock.fixed(Instant.parse("2026-10-14T03:00:00Z"), ZoneOffset.UTC));

    @Test
    void FR01_duplicateCourseCodeIsConflict() {
        service.create("alice", "CSCI318", "Software Engineering");

        assertThatThrownBy(() -> service.create("alice", " csci318 ", "Again")).isInstanceOf(ConflictException.class);
        assertThat(service.create("bob", "CSCI318", "SE").code().value()).as("other students are not affected")
                .isEqualTo("CSCI318");
    }

    @Test
    void FR01_otherStudentsCourseIsNotFound() {
        UUID id = service.create("alice", "CSCI318", "SE").id();

        assertThatThrownBy(() -> service.get("bob", id)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.addTopic("bob", id, "Kafka")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete("bob", id)).isInstanceOf(NotFoundException.class);
        assertThat(service.list("bob")).isEmpty();
    }

    @Test
    void FR01_eventsPublishedOnlyAfterSave() {
        UUID id = service.create("alice", "CSCI318", "SE").id();
        UUID topicId = service.addTopic("alice", id, "Kafka").id();
        service.deleteTopic("alice", id, topicId);
        service.addTopic("alice", id, "DDD");

        service.delete("alice", id);

        assertThat(published).extracting(DomainEvent::eventType)
                .containsExactly("TopicCreated", "TopicDeleted", "TopicCreated", "TopicDeleted");
        assertThat(courseStoredWhenPublished).as("course saved before each create/delete-topic event")
                .containsExactly(true, true, true, false);
        assertThat(courses.byId).as("course deleted before its TopicDeleted events go out").isEmpty();
    }

    @Test
    void FR02_materialForUnknownTopicIsIgnored() {
        UUID id = service.create("alice", "CSCI318", "SE").id();
        UUID topicId = service.addTopic("alice", id, "Kafka").id();

        service.onMaterialAdded(topicId, UUID.randomUUID());
        service.onMaterialAdded(UUID.randomUUID(), UUID.randomUUID());

        assertThat(service.get("alice", id).topics()).singleElement()
                .satisfies(t -> assertThat(t.materialCount()).isEqualTo(1));
    }

    @Test
    void FR07_understandingForUnknownTopicIgnored() {
        UUID id = service.create("alice", "CSCI318", "SE").id();
        UUID topicId = service.addTopic("alice", id, "Kafka").id();

        service.onUnderstandingRecorded(topicId, 3, Instant.parse("2026-10-14T03:00:00Z"));
        service.onUnderstandingRecorded(UUID.randomUUID(), 5, Instant.parse("2026-10-14T03:00:00Z"));

        assertThat(service.get("alice", id).topics()).singleElement()
                .satisfies(t -> assertThat(t.understandingLevel()).isEqualTo(3));
    }
}
