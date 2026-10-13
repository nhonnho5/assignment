package com.studyanchor.social.service;

import com.studyanchor.social.domain.FriendshipRepository;
import com.studyanchor.social.domain.NotFoundException;
import com.studyanchor.social.domain.Presence;
import com.studyanchor.social.domain.PresenceBoard;
import com.studyanchor.social.domain.SharingSettingRepository;
import com.studyanchor.social.domain.StudyRoom;
import com.studyanchor.social.domain.StudyRoomRepository;
import com.studyanchor.social.domain.StudyingNow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements FR-09. Application service for the live "studying now" board: combines presence from the stream's
 * state store with friendships, rooms and sharing settings from the database. The sharing filter runs here,
 * at query time, so turning sharing off takes effect immediately (AC5).
 */
@Service
public class StudyingNowService {

    /** One friend on the board (FR-09 AC1, AC8, AC9). */
    public record FriendStudying(String userId, UUID topicId, Instant startedAt, Duration elapsed) {
    }

    /** A room's live count (FR-09 AC2, AC6). */
    public record RoomStudying(UUID roomId, String roomName, int studyingCount, int memberCount) {
    }

    private final FriendshipRepository friendships;
    private final StudyRoomRepository rooms;
    private final SharingSettingRepository sharing;
    private final PresenceBoard presenceBoard;
    private final Clock clock;

    public StudyingNowService(FriendshipRepository friendships, StudyRoomRepository rooms,
                              SharingSettingRepository sharing, PresenceBoard presenceBoard, Clock clock) {
        this.friendships = friendships;
        this.rooms = rooms;
        this.sharing = sharing;
        this.presenceBoard = presenceBoard;
        this.clock = clock;
    }

    /** FR-09 AC1, AC5, AC10: friends studying now who share their presence; the viewer's own setting is irrelevant. */
    @Transactional(readOnly = true)
    public List<FriendStudying> friendsStudying(String userId) {
        List<String> friendIds = friendships.findAcceptedFor(userId).stream().map(f -> f.otherThan(userId)).toList();
        Map<String, Presence> presences = presenceBoard.presenceOf(friendIds);
        Instant now = clock.instant();
        return StudyingNow.friendsStudying(friendIds, presences, sharing.sharingAmong(presences.keySet())).stream()
                .map(p -> new FriendStudying(p.userId(), p.topicId(), p.startedAt(), p.elapsed(now)))
                .toList();
    }

    /** FR-09 AC2, AC6, AC7: members only (else 404); counts every member studying, whatever their sharing. */
    @Transactional(readOnly = true)
    public RoomStudying roomStudying(String userId, UUID roomId) {
        StudyRoom room = rooms.findById(roomId)
                .filter(r -> r.isMember(userId))
                .orElseThrow(() -> new NotFoundException("Study room " + roomId + " not found"));
        int studying = StudyingNow.countStudying(room.members(), presenceBoard.presenceOf(room.members()));
        return new RoomStudying(room.id(), room.name().value(), studying, room.members().size());
    }
}
