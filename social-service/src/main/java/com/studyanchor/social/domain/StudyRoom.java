package com.studyanchor.social.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Implements FR-08 AC2, AC7. Aggregate root: a named group of students who study together.
 * Members are part of the aggregate, so they can only be added through {@link #join}.
 */
@Entity
@Table(name = "study_room")
public class StudyRoom {

    @Id
    private UUID id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "name", nullable = false))
    private RoomName name;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "study_room_member", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "user_id")
    private Set<String> members = new LinkedHashSet<>();

    /** For JPA only. */
    protected StudyRoom() {
    }

    /** FR-08 AC2, AC7: creates a room; the creator is its first member. */
    public static StudyRoom create(String name, String creatorId, Instant now) {
        if (creatorId == null || creatorId.isBlank()) {
            throw new DomainException("userId is required");
        }
        StudyRoom room = new StudyRoom();
        room.id = UUID.randomUUID();
        room.name = new RoomName(name);
        room.createdBy = creatorId;
        room.createdAt = now;
        room.members.add(creatorId);
        return room;
    }

    /** FR-08 AC2, AC7: anyone can join by room ID, but only once (409). */
    public void join(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new DomainException("userId is required");
        }
        if (!members.add(userId)) {
            throw new ConflictException("User " + userId + " is already a member of room " + id);
        }
    }

    public boolean isMember(String userId) {
        return members.contains(userId);
    }

    public UUID id() {
        return id;
    }

    public RoomName name() {
        return name;
    }

    public String createdBy() {
        return createdBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Set<String> members() {
        return Set.copyOf(members);
    }
}
