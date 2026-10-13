package com.studyanchor.social.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements FR-08. Repository for the {@link Friendship} aggregate; implemented in infrastructure. */
public interface FriendshipRepository {

    Friendship save(Friendship friendship);

    Optional<Friendship> findById(UUID id);

    /** FR-08 AC5: true if a pending request or a friendship exists between the two users, in either direction. */
    boolean existsBetween(String a, String b);

    /** Accepted friendships the user is part of, as requester or recipient. */
    List<Friendship> findAcceptedFor(String userId);

    /** Pending requests waiting for this user to accept. */
    List<Friendship> findPendingFor(String recipientId);
}
