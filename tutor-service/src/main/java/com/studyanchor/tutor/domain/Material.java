package com.studyanchor.tutor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implements FR-02. Aggregate root: study material (title + text) a student added to one of their topics.
 * There is no edit or delete (AC6); it registers {@link MaterialAdded} for the service to publish after saving.
 */
@Entity
@Table(name = "material")
public class Material {

    static final int MAX_CONTENT = 50_000;

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private UUID topicId;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Instant addedAt;

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /** For JPA only. */
    protected Material() {
    }

    /**
     * FR-02 AC4, AC5: the title defaults to the file name without its extension; title 1–100 and
     * content 1–50,000 characters, both trimmed. Blank content means the file (e.g. a scanned PDF) has no text.
     */
    public static Material create(String userId, UUID topicId, String fileName, String title, String content,
                                  Instant now) {
        if (content == null || content.isBlank()) {
            throw new DomainException("The file has no text (a scanned PDF has only images)");
        }
        Material m = new Material();
        m.id = UUID.randomUUID();
        m.userId = userId;
        m.topicId = topicId;
        m.title = Text.of(title == null || title.isBlank() ? withoutExtension(fileName) : title, "Title", 100);
        m.content = Text.of(content, "Content", MAX_CONTENT);
        m.addedAt = now;
        m.domainEvents.add(MaterialAdded.of(m));
        return m;
    }

    private static String withoutExtension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }

    /** Returns the events registered since the last call and forgets them, so each is published once. */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public UUID id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public UUID topicId() {
        return topicId;
    }

    public String title() {
        return title;
    }

    public String content() {
        return content;
    }

    public Instant addedAt() {
        return addedAt;
    }
}
