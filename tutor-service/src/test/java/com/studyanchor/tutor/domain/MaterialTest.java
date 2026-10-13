package com.studyanchor.tutor.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Domain rules for FR-02 study material. Plain Java, no Spring. */
class MaterialTest {

    private static final Instant NOW = Instant.parse("2026-10-14T03:00:00Z");
    private static final UUID TOPIC = UUID.randomUUID();

    private static Material material(String fileName, String title, String content) {
        return Material.create("alice", TOPIC, fileName, title, content, NOW);
    }

    @Test
    void FR02_titleDefaultsToFileNameWithoutExtension() {
        assertThat(material("week1-notes.md", null, "Kafka").title()).isEqualTo("week1-notes");
        assertThat(material("week1.notes.pdf", " ", "Kafka").title()).isEqualTo("week1.notes");
        assertThat(material("notes.txt", "  Kafka basics ", "Kafka").title()).isEqualTo("Kafka basics");
    }

    @Test
    void FR02_titleMax100Characters() {
        assertThat(material("a.md", "x".repeat(100), "Kafka").title()).hasSize(100);
        assertThatThrownBy(() -> material("a.md", "x".repeat(101), "Kafka")).isInstanceOf(DomainException.class);
    }

    @Test
    void FR02_blankContentRejected() {
        assertThatThrownBy(() -> material("a.md", null, " \n\t ")).isInstanceOf(DomainException.class);
    }

    @Test
    void FR02_pdfWithoutTextIsRejected() {
        assertThatThrownBy(() -> material("scan.pdf", null, "\n\n"))
                .isInstanceOf(DomainException.class).hasMessageContaining("no text");
    }

    @Test
    void FR02_contentMax50000Characters() {
        assertThat(material("a.md", null, " " + "x".repeat(50_000) + "\n").content()).hasSize(50_000);
        assertThatThrownBy(() -> material("a.md", null, "x".repeat(50_001))).isInstanceOf(DomainException.class);
    }

    @Test
    void FR02_addingRegistersMaterialAdded() {
        Material m = material("notes.md", "Kafka basics", "Kafka is a log");

        assertThat(m.pullDomainEvents()).singleElement().isInstanceOfSatisfying(MaterialAdded.class, e -> {
            assertThat(e.eventType()).isEqualTo("MaterialAdded");
            assertThat(e.userId()).isEqualTo("alice");
            assertThat(e.materialId()).isEqualTo(m.id());
            assertThat(e.topicId()).isEqualTo(TOPIC);
            assertThat(e.title()).isEqualTo("Kafka basics");
            assertThat(e.occurredAt()).isEqualTo(NOW);
        });
        assertThat(m.pullDomainEvents()).as("each event is published once").isEmpty();
    }
}
