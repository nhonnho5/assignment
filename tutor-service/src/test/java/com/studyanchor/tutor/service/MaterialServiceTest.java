package com.studyanchor.tutor.service;

import com.studyanchor.tutor.domain.DomainEvent;
import com.studyanchor.tutor.domain.Material;
import com.studyanchor.tutor.domain.MaterialAdded;
import com.studyanchor.tutor.domain.MaterialRepository;
import com.studyanchor.tutor.domain.NotFoundException;
import com.studyanchor.tutor.domain.TopicReference;
import com.studyanchor.tutor.service.TopicCatalogServiceTest.InMemoryTopics;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Application-service rules for FR-02, using in-memory fakes (no Spring, no Kafka, no PDFBox). */
class MaterialServiceTest {

    /** Keeps materials in a map instead of a database. */
    static class InMemoryMaterials implements MaterialRepository {

        final Map<UUID, Material> byId = new HashMap<>();

        @Override
        public Material save(Material material) {
            byId.put(material.id(), material);
            return material;
        }

        @Override
        public Optional<Material> findByIdAndUserId(UUID id, String userId) {
            return Optional.ofNullable(byId.get(id)).filter(m -> m.userId().equals(userId));
        }

        @Override
        public List<Material> findByTopicId(UUID topicId) {
            return byId.values().stream().filter(m -> m.topicId().equals(topicId))
                    .sorted(Comparator.comparing(Material::addedAt)).toList();
        }

        @Override
        public void deleteByTopicId(UUID topicId) {
            byId.values().removeIf(m -> m.topicId().equals(topicId));
        }
    }

    private final InMemoryTopics topics = new InMemoryTopics();
    private final InMemoryMaterials materials = new InMemoryMaterials();
    private final List<DomainEvent> published = new ArrayList<>();
    private final MaterialService service = new MaterialService(materials, topics,
            (fileName, bytes) -> new String(bytes, StandardCharsets.UTF_8),
            event -> {
                assertThat(materials.byId).as("saved before publishing").isNotEmpty();
                published.add(event);
            },
            Clock.fixed(Instant.parse("2026-10-14T03:00:00Z"), ZoneOffset.UTC));

    private final UUID topicId = topics.save(
            new TopicReference(UUID.randomUUID(), "alice", UUID.randomUUID(), "CSCI318", "Kafka")).topicId();

    private Material add(String userId, UUID topic) {
        return service.add(userId, topic, "notes.md", "Kafka is a log".getBytes(StandardCharsets.UTF_8), null);
    }

    @Test
    void FR02_unknownTopicIsNotFound() {
        assertThatThrownBy(() -> add("alice", UUID.randomUUID())).isInstanceOf(NotFoundException.class);
        assertThat(published).isEmpty();
    }

    @Test
    void FR02_otherStudentsTopicIsNotFound() {
        assertThatThrownBy(() -> add("bob", topicId)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.list("bob", topicId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void FR02_eventPublishedOnlyAfterSave() {
        Material m = add("alice", topicId);

        assertThat(m.title()).isEqualTo("notes");
        assertThat(published).singleElement().isInstanceOfSatisfying(MaterialAdded.class,
                e -> assertThat(e.materialId()).isEqualTo(m.id()));
        assertThat(service.list("alice", topicId)).containsExactly(m);
    }

    @Test
    void FR02_otherStudentsMaterialIsNotFound() {
        Material m = add("alice", topicId);

        assertThat(service.get("alice", m.id())).isSameAs(m);
        assertThatThrownBy(() -> service.get("bob", m.id())).isInstanceOf(NotFoundException.class);
    }
}
