package com.studyanchor.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Implements FR-08 AC1, AC5, AC6. Aggregate root: a friend request between two students, which becomes a
 * mutual friendship once the recipient accepts it. Changed only through {@link #request} and {@link #accept}.
 */
@Entity
@Table(name = "friendship")
public class Friendship {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String requesterId;

    @Column(nullable = false)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatus status;

    @Column(nullable = false)
    private Instant requestedAt;

    private Instant acceptedAt;

    /** For JPA only. */
    protected Friendship() {
    }

    /** FR-08 AC1, AC4, AC5: any two different, non-blank user IDs (there is no user registry). */
    public static Friendship request(String requesterId, String recipientId, Instant now) {
        if (requesterId == null || requesterId.isBlank() || recipientId == null || recipientId.isBlank()) {
            throw new DomainException("Both user IDs are required");
        }
        if (requesterId.equals(recipientId)) {
            throw new DomainException("You cannot send a friend request to yourself");
        }
        Friendship friendship = new Friendship();
        friendship.id = UUID.randomUUID();
        friendship.requesterId = requesterId;
        friendship.recipientId = recipientId;
        friendship.status = FriendshipStatus.PENDING;
        friendship.requestedAt = now;
        return friendship;
    }

    /**
     * FR-08 AC6: only the recipient can accept. To anyone else the request looks like it does not exist (404).
     * Accepting twice is a conflict (409).
     */
    public void accept(String userId, Instant now) {
        if (!recipientId.equals(userId)) {
            throw new NotFoundException("Friend request " + id + " not found");
        }
        if (status == FriendshipStatus.ACCEPTED) {
            throw new ConflictException("Friend request " + id + " has already been accepted");
        }
        status = FriendshipStatus.ACCEPTED;
        acceptedAt = now;
    }

    /** True if this request or friendship links these two users, in either direction. */
    public boolean isBetween(String a, String b) {
        return (requesterId.equals(a) && recipientId.equals(b)) || (requesterId.equals(b) && recipientId.equals(a));
    }

    /** The friend on the other side, seen from {@code userId}. */
    public String otherThan(String userId) {
        return requesterId.equals(userId) ? recipientId : requesterId;
    }

    public UUID id() {
        return id;
    }

    public String requesterId() {
        return requesterId;
    }

    public String recipientId() {
        return recipientId;
    }

    public FriendshipStatus status() {
        return status;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public Instant acceptedAt() {
        return acceptedAt;
    }
}
