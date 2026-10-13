package com.studyanchor.social.service;

import com.studyanchor.social.domain.ConflictException;
import com.studyanchor.social.domain.DomainException;
import com.studyanchor.social.domain.Friendship;
import com.studyanchor.social.domain.NotFoundException;
import com.studyanchor.social.domain.Presence;
import com.studyanchor.social.domain.StudyRoom;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Application-service rules for FR-08 and FR-09, using in-memory fakes (no Spring, no Kafka). */
class SocialServicesTest {

    private static final Instant NOW = Instant.parse("2026-10-14T03:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryFakes.Friendships friendships = new InMemoryFakes.Friendships();
    private final InMemoryFakes.Rooms rooms = new InMemoryFakes.Rooms();
    private final InMemoryFakes.Sharing sharing = new InMemoryFakes.Sharing();
    private final InMemoryFakes.Board board = new InMemoryFakes.Board();

    private final FriendshipService friendshipService = new FriendshipService(friendships, clock);
    private final StudyRoomService roomService = new StudyRoomService(rooms, clock);
    private final SharingService sharingService = new SharingService(sharing);
    private final StudyingNowService studyingNow =
            new StudyingNowService(friendships, rooms, sharing, board, clock);

    private void makeFriends(String a, String b) {
        Friendship request = friendshipService.sendRequest(a, b);
        friendshipService.accept(b, request.id());
    }

    private void startStudying(String userId, Duration ago) {
        board.current.put(userId, new Presence(userId, UUID.randomUUID(), null, NOW.minus(ago)));
    }

    // ---- FR-08 friends ----

    @Test
    void FR08_cannotRequestWhenFriendsOrPendingEitherWay() {
        friendshipService.sendRequest("alice", "bob");

        assertThatThrownBy(() -> friendshipService.sendRequest("alice", "bob")).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> friendshipService.sendRequest("bob", "alice")).isInstanceOf(ConflictException.class);

        makeFriends("carol", "dave");
        assertThatThrownBy(() -> friendshipService.sendRequest("dave", "carol")).isInstanceOf(ConflictException.class);
    }

    @Test
    void FR08_cannotRequestYourself() {
        assertThatThrownBy(() -> friendshipService.sendRequest("alice", "alice"))
                .isInstanceOf(DomainException.class)
                .isNotInstanceOf(ConflictException.class);
    }

    @Test
    void FR08_recipientSeesAndAcceptsIncomingRequest() {
        Friendship request = friendshipService.sendRequest("alice", "bob");

        assertThat(friendshipService.incomingRequests("bob")).extracting(Friendship::id).containsExactly(request.id());
        assertThat(friendshipService.incomingRequests("alice")).isEmpty();

        friendshipService.accept("bob", request.id());

        assertThat(friendshipService.incomingRequests("bob")).isEmpty();
        assertThat(friendshipService.friendsOf("alice")).containsExactly("bob");
        assertThat(friendshipService.friendsOf("bob")).containsExactly("alice");
    }

    @Test
    void FR08_acceptingUnknownRequestIsNotFound() {
        assertThatThrownBy(() -> friendshipService.accept("bob", UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- FR-08 rooms and sharing ----

    @Test
    void FR08_joinUnknownRoomIsNotFound() {
        assertThatThrownBy(() -> roomService.join("bob", UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void FR08_othersJoinByRoomIdAndOnlyMembersSeeTheRoom() {
        StudyRoom room = roomService.create("alice", "Library");

        assertThatThrownBy(() -> roomService.get("bob", room.id())).isInstanceOf(NotFoundException.class);

        roomService.join("bob", room.id());
        assertThat(roomService.get("bob", room.id()).members()).containsExactlyInAnyOrder("alice", "bob");
        assertThatThrownBy(() -> roomService.join("bob", room.id())).isInstanceOf(ConflictException.class);
    }

    @Test
    void FR08_sharingIsOffUntilTurnedOn() {
        assertThat(sharingService.get("alice").isSharing()).isFalse();

        sharingService.set("alice", true);
        assertThat(sharingService.get("alice").isSharing()).isTrue();

        sharingService.set("alice", false);
        assertThat(sharingService.get("alice").isSharing()).isFalse();
    }

    // ---- FR-09 studying now ----

    @Test
    void FR09_friendsBoardShowsElapsedTimeAndTopic() {
        makeFriends("alice", "bob");
        sharingService.set("bob", true);
        UUID topicId = UUID.randomUUID();
        board.current.put("bob", new Presence("bob", UUID.randomUUID(), topicId, NOW.minusSeconds(725)));

        List<StudyingNowService.FriendStudying> result = studyingNow.friendsStudying("alice");

        assertThat(result).singleElement().satisfies(f -> {
            assertThat(f.userId()).isEqualTo("bob");
            assertThat(f.topicId()).isEqualTo(topicId);
            assertThat(f.elapsed()).isEqualTo(Duration.ofSeconds(725));
        });
    }

    @Test
    void FR09_turningSharingOffTakesEffectImmediately() {
        makeFriends("alice", "bob");
        sharingService.set("bob", true);
        startStudying("bob", Duration.ofMinutes(5));
        assertThat(studyingNow.friendsStudying("alice")).hasSize(1);

        sharingService.set("bob", false);

        assertThat(studyingNow.friendsStudying("alice")).isEmpty();
    }

    @Test
    void FR09_viewerDoesNotNeedSharingOnToSeeFriends() {
        makeFriends("alice", "bob");
        sharingService.set("bob", true);
        startStudying("bob", Duration.ofMinutes(5));

        assertThat(sharingService.get("alice").isSharing()).isFalse();
        assertThat(studyingNow.friendsStudying("alice")).hasSize(1);
    }

    @Test
    void FR09_roomCountIncludesMembersWithSharingOff() {
        StudyRoom room = roomService.create("alice", "Library");
        roomService.join("bob", room.id());
        roomService.join("carol", room.id());
        startStudying("bob", Duration.ofMinutes(5));
        startStudying("carol", Duration.ofMinutes(1));
        sharingService.set("carol", true);

        StudyingNowService.RoomStudying result = studyingNow.roomStudying("alice", room.id());

        assertThat(result.studyingCount()).isEqualTo(2);
        assertThat(result.memberCount()).isEqualTo(3);
    }

    @Test
    void FR09_onlyMembersSeeRoomCount() {
        StudyRoom room = roomService.create("alice", "Library");

        assertThatThrownBy(() -> studyingNow.roomStudying("mallory", room.id())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> studyingNow.roomStudying("alice", UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }
}
