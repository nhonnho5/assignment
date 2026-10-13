package com.studyanchor.social.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Domain rules for FR-08 friendships. Plain Java, no Spring. */
class FriendshipTest {

    private static final Instant NOW = Instant.parse("2026-10-14T03:00:00Z");

    @Test
    void FR08_cannotBefriendYourself() {
        assertThatThrownBy(() -> Friendship.request("alice", "alice", NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Friendship.request("alice", " ", NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Friendship.request(null, "bob", NOW)).isInstanceOf(DomainException.class);
    }

    @Test
    void FR08_requestStartsPending() {
        Friendship request = Friendship.request("alice", "bob", NOW);

        assertThat(request.status()).isEqualTo(FriendshipStatus.PENDING);
        assertThat(request.requesterId()).isEqualTo("alice");
        assertThat(request.recipientId()).isEqualTo("bob");
        assertThat(request.isBetween("bob", "alice")).as("either direction").isTrue();
    }

    @Test
    void FR08_onlyRecipientCanAccept() {
        Friendship request = Friendship.request("alice", "bob", NOW);

        assertThatThrownBy(() -> request.accept("alice", NOW)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> request.accept("carol", NOW)).isInstanceOf(NotFoundException.class);

        request.accept("bob", NOW);
        assertThat(request.status()).isEqualTo(FriendshipStatus.ACCEPTED);
        assertThat(request.acceptedAt()).isEqualTo(NOW);
    }

    @Test
    void FR08_cannotAcceptTwice() {
        Friendship request = Friendship.request("alice", "bob", NOW);
        request.accept("bob", NOW);

        assertThatThrownBy(() -> request.accept("bob", NOW)).isInstanceOf(ConflictException.class);
    }

    @Test
    void FR08_friendshipIsMutualOnceAccepted() {
        Friendship friendship = Friendship.request("alice", "bob", NOW);
        friendship.accept("bob", NOW);

        assertThat(friendship.otherThan("alice")).isEqualTo("bob");
        assertThat(friendship.otherThan("bob")).isEqualTo("alice");
    }
}
