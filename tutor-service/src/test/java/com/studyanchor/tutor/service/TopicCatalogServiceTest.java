package com.studyanchor.tutor.service;

import com.studyanchor.tutor.domain.Material;
import com.studyanchor.tutor.domain.TopicReference;
import com.studyanchor.tutor.domain.TopicReferenceRepository;
import com.studyanchor.tutor.service.MaterialServiceTest.InMemoryMaterials;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            return existsById(topicId) && byId.get(topicId).userId().equals(userId);
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
    private final TopicCatalogService service = new TopicCatalogService(topics, materials);

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
}
