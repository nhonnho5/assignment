package com.studyanchor.social.presentation;

import com.studyanchor.social.domain.Friendship;
import com.studyanchor.social.domain.SharingSetting;
import com.studyanchor.social.domain.StudyRoom;
import com.studyanchor.social.service.StudyingNowService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Implements FR-08, FR-09. The JSON shapes of the Social API: requests in, responses out. No logic. */
final class SocialDtos {

    private SocialDtos() {
    }

    // ---- requests ----

    record FriendRequestRequest(String toUserId) {
    }

    record CreateRoomRequest(String name) {
    }

    record SharingRequest(Boolean sharing) {
    }

    // ---- responses ----

    record FriendRequestResponse(UUID id, String fromUserId, String toUserId, String status,
                                 Instant requestedAt, Instant acceptedAt) {

        static FriendRequestResponse from(Friendship f) {
            return new FriendRequestResponse(f.id(), f.requesterId(), f.recipientId(), f.status().name(),
                    f.requestedAt(), f.acceptedAt());
        }
    }

    record FriendsResponse(String userId, List<String> friends) {
    }

    record RoomResponse(UUID id, String name, String createdBy, Instant createdAt, List<String> members) {

        static RoomResponse from(StudyRoom room) {
            return new RoomResponse(room.id(), room.name().value(), room.createdBy(), room.createdAt(),
                    room.members().stream().sorted().toList());
        }
    }

    record SharingResponse(String userId, boolean sharing) {

        static SharingResponse from(SharingSetting setting) {
            return new SharingResponse(setting.userId(), setting.isSharing());
        }
    }

    /** FR-09 AC1, AC8: elapsed as minutes and seconds (same format as FR-05) plus total seconds. */
    record FriendStudyingResponse(String userId, UUID topicId, Instant startedAt, String elapsed,
                                  long elapsedSeconds) {

        static FriendStudyingResponse from(StudyingNowService.FriendStudying f) {
            Duration e = f.elapsed();
            return new FriendStudyingResponse(f.userId(), f.topicId(), f.startedAt(),
                    e.toMinutes() + "m " + e.toSecondsPart() + "s", e.toSeconds());
        }
    }

    record StudyingNowResponse(String userId, List<FriendStudyingResponse> friendsStudying) {
    }

    record RoomStudyingResponse(UUID roomId, String roomName, int studyingCount, int memberCount) {

        static RoomStudyingResponse from(StudyingNowService.RoomStudying r) {
            return new RoomStudyingResponse(r.roomId(), r.roomName(), r.studyingCount(), r.memberCount());
        }
    }
}
