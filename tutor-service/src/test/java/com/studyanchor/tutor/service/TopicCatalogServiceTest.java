package com.studyanchor.tutor.service;

import com.studyanchor.tutor.domain.Material;
import com.studyanchor.tutor.domain.TopicReference;
import com.studyanchor.tutor.domain.TopicReferenceRepository;
import com.studyanchor.tutor.domain.TutoringSession;
import com.studyanchor.tutor.service.MaterialServiceTest.InMemoryMaterials;
import com.studyanchor.tutor.service.TutoringServiceTest.InMemorySessions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Tutor's copy of Planner topics (FR-01 AC10), using an in-memory fake (no Spring, no Kafka). */
class TopicCatalogServiceTest {

    /** Keeps topic copies in a map instead of a database. */
    static class InMemoryTopics implements TopicReferenceRepository {

        final Map<UUID, TopicReference> byId = new HashMap<>();

        @Override
        public TopicReference save(TopicReference topic) {
            byId.put(topic.topicId(), topic);
            return topic;
        }

        @Override
        public boolean existsById(UUID topicId) {
            return byId.containsKey(topicId);
        }

        @Override
        public boolean existsByIdAndUserId(UUID topicId, String userId) {
            return findByIdAndUserId(topicId, userId).isPresent();
        }

        @Override
        public Optional<TopicReference> findByIdAndUserId(UUID topicId, String userId) {
            return Optional.ofNullable(byId.get(topicId)).filter(t -> t.userId().equals(userId));
        }

        @Override
        public List<TopicReference> findByUserId(String userId) {
            return byId.values().stream().filter(t -> t.userId().equals(userId)).toList();
        }

        @Override
        public void deleteById(UUID topicId) {
            byId.remove(topicId);
        }
    }

    private final InMemoryTopics topics = new InMemoryTopics();
    private final InMemoryMaterials materials = new InMemoryMaterials();
    private final InMemorySessions sessions = new InMemorySessions();
    private final TopicCatalogService service = new TopicCatalogService(topics, materials, sessions);

    private static TopicReference kafka(UUID id) {
        return new TopicReference(id, "alice", UUID.randomUUID(), "CSCI318", "Kafka");
    }

    @Test
    void FR01_topicCreatedStoredOnce() {
        UUID id = UUID.randomUUID();

        service.onTopicCreated(kafka(id));
        service.onTopicCreated(kafka(id));

        assertThat(service.topicsOf("alice")).singleElement().satisfies(t -> {
            assertThat(t.topicId()).isEqualTo(id);
            assertThat(t.courseCode()).isEqualTo("CSCI318");
            assertThat(t.topicName()).isEqualTo("Kafka");
        });
        assertThat(service.topicsOf("bob")).isEmpty();
    }

    @Test
    void FR01_topicDeletedRemovesTopic() {
        UUID id = UUID.randomUUID();
        service.onTopicCreated(kafka(id));

        service.onTopicDeleted(id);
        service.onTopicDeleted(UUID.randomUUID());

        assertThat(service.topicsOf("alice")).as("unknown TopicDeleted is ignored").isEmpty();
    }

    @Test
    void FR02_topicDeletedRemovesItsMaterials() {
        UUID id = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-14T03:00:00Z");
        materials.save(Material.create("alice", id, "a.md", null, "Kafka", now));
        materials.save(Material.create("alice", other, "b.md", null, "Streams", now));

        service.onTopicDeleted(id);

        assertThat(materials.byId.values()).singleElement().satisfies(m -> assertThat(m.topicId()).isEqualTo(other));
    }

    @Test
    void FR07_topicDeletedRemovesItsTutoringSessions() {
        UUID id = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-14T03:00:00Z");
        sessions.save(TutoringSession.start("alice", id, "What is a partition?", now));
        sessions.save(TutoringSession.start("alice", other, "What is a stream?", now));

        service.onTopicDeleted(id);

        assertThat(sessions.byId.values()).singleElement().satisfies(s -> assertThat(s.topicId()).isEqualTo(other));
    }
}
