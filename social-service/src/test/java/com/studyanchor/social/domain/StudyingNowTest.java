package com.studyanchor.social.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Domain rules for FR-09 (live "studying now" board). Plain Java, no Spring. */
class StudyingNowTest {

    private static final Instant NOW = Instant.parse("2026-10-14T03:00:00Z");

    private static Presence studying(String userId, Duration ago) {
        return new Presence(userId, UUID.randomUUID(), null, NOW.minus(ago));
    }

    @Test
    void FR09_listsFriendsInASessionWithSharingOn() {
        Presence bob = studying("bob", Duration.ofMinutes(12));

        List<Presence> board = StudyingNow.friendsStudying(
                List.of("bob", "carol"), Map.of("bob", bob), Set.of("bob", "carol"));

        assertThat(board).containsExactly(bob);
    }

    @Test
    void FR09_sharingOffNeverAppearsOnFriendsBoard() {
        Presence bob = studying("bob", Duration.ofMinutes(12));
        Presence carol = studying("carol", Duration.ofMinutes(3));

        List<Presence> board = StudyingNow.friendsStudying(
                List.of("bob", "carol"), Map.of("bob", bob, "carol", carol), Set.of("carol"));

        assertThat(board).containsExactly(carol);
    }

    @Test
    void FR09_onlyFriendsAppear() {
        Presence stranger = studying("mallory", Duration.ofMinutes(5));

        List<Presence> board = StudyingNow.friendsStudying(
                List.of("bob"), Map.of("mallory", stranger), Set.of("mallory"));

        assertThat(board).isEmpty();
    }

    @Test
    void FR09_friendsBoardIsOrderedByLongestStudyingFirst() {
        Presence bob = studying("bob", Duration.ofMinutes(5));
        Presence carol = studying("carol", Duration.ofMinutes(40));

        List<Presence> board = StudyingNow.friendsStudying(
                List.of("bob", "carol"), Map.of("bob", bob, "carol", carol), Set.of("bob", "carol"));

        assertThat(board).containsExactly(carol, bob);
    }

    @Test
    void FR09_roomCountIncludesMembersWithSharingOff() {
        Map<String, Presence> presences = Map.of(
                "alice", studying("alice", Duration.ofMinutes(1)),
                "bob", studying("bob", Duration.ofMinutes(2)),
                "outsider", studying("outsider", Duration.ofMinutes(3)));

        assertThat(StudyingNow.countStudying(Set.of("alice", "bob", "carol"), presences)).isEqualTo(2);
    }

    @Test
    void FR09_elapsedComputedFromStartedAt() {
        Presence bob = studying("bob", Duration.ofMinutes(12).plusSeconds(5).plusMillis(900));

        assertThat(bob.elapsed(NOW)).isEqualTo(Duration.ofMinutes(12).plusSeconds(5));
    }

    @Test
    void FR09_elapsedIsNeverNegative() {
        Presence clockSkew = new Presence("bob", UUID.randomUUID(), null, NOW.plusSeconds(3));

        assertThat(clockSkew.elapsed(NOW)).isEqualTo(Duration.ZERO);
    }
}
