package com.studyanchor.social.service;

import com.studyanchor.social.domain.Friendship;
import com.studyanchor.social.domain.FriendshipRepository;
import com.studyanchor.social.domain.FriendshipStatus;
import com.studyanchor.social.domain.Presence;
import com.studyanchor.social.domain.PresenceBoard;
import com.studyanchor.social.domain.SharingSetting;
import com.studyanchor.social.domain.SharingSettingRepository;
import com.studyanchor.social.domain.StudyRoom;
import com.studyanchor.social.domain.StudyRoomRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Test fakes: keep data in maps instead of a database or a Kafka Streams store. */
final class InMemoryFakes {

    private InMemoryFakes() {
    }

    static class Friendships implements FriendshipRepository {

        private final Map<UUID, Friendship> byId = new HashMap<>();

        @Override
        public Friendship save(Friendship friendship) {
            byId.put(friendship.id(), friendship);
            return friendship;
        }

        @Override
        public Optional<Friendship> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public boolean existsBetween(String a, String b) {
            return byId.values().stream().anyMatch(f -> f.isBetween(a, b));
        }

        @Override
        public List<Friendship> findAcceptedFor(String userId) {
            return byId.values().stream()
                    .filter(f -> f.status() == FriendshipStatus.ACCEPTED)
                    .filter(f -> f.requesterId().equals(userId) || f.recipientId().equals(userId))
                    .toList();
        }

        @Override
        public List<Friendship> findPendingFor(String recipientId) {
            return byId.values().stream()
                    .filter(f -> f.status() == FriendshipStatus.PENDING && f.recipientId().equals(recipientId))
                    .toList();
        }
    }

    static class Rooms implements StudyRoomRepository {

        private final Map<UUID, StudyRoom> byId = new HashMap<>();

        @Override
        public StudyRoom save(StudyRoom room) {
            byId.put(room.id(), room);
            return room;
        }

        @Override
        public Optional<StudyRoom> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }
    }

    static class Sharing implements SharingSettingRepository {

        private final Map<String, SharingSetting> byUser = new HashMap<>();

        @Override
        public SharingSetting save(SharingSetting setting) {
            byUser.put(setting.userId(), setting);
            return setting;
        }

        @Override
        public Optional<SharingSetting> findByUserId(String userId) {
            return Optional.ofNullable(byUser.get(userId));
        }

        @Override
        public Set<String> sharingAmong(Collection<String> userIds) {
            return userIds.stream()
                    .filter(id -> byUser.containsKey(id) && byUser.get(id).isSharing())
                    .collect(Collectors.toSet());
        }
    }

    /** Stands in for the presence state store: tests put and remove presences directly. */
    static class Board implements PresenceBoard {

        final Map<String, Presence> current = new HashMap<>();

        @Override
        public Map<String, Presence> presenceOf(Collection<String> userIds) {
            return userIds.stream().filter(current::containsKey)
                    .collect(Collectors.toMap(id -> id, current::get));
        }
    }
}
