package com.studyanchor.social.service;

import com.studyanchor.social.domain.NotFoundException;
import com.studyanchor.social.domain.StudyRoom;
import com.studyanchor.social.domain.StudyRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** Implements FR-08 AC2, AC7. Application service for study rooms: load or create, call the aggregate, save. */
@Service
public class StudyRoomService {

    private final StudyRoomRepository repository;
    private final Clock clock;

    public StudyRoomService(StudyRoomRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public StudyRoom create(String userId, String name) {
        return repository.save(StudyRoom.create(name, userId, clock.instant()));
    }

    /** FR-08 AC2, AC7: unknown room → 404; already a member → 409 (from the aggregate). */
    @Transactional
    public StudyRoom join(String userId, UUID roomId) {
        StudyRoom room = find(roomId);
        room.join(userId);
        return repository.save(room);
    }

    /** Room details for members only; non-members get 404 (FR-09 AC7 applies the same rule to the count). */
    @Transactional(readOnly = true)
    public StudyRoom get(String userId, UUID roomId) {
        StudyRoom room = find(roomId);
        if (!room.isMember(userId)) {
            throw new NotFoundException("Study room " + roomId + " not found");
        }
        return room;
    }

    private StudyRoom find(UUID roomId) {
        return repository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Study room " + roomId + " not found"));
    }
}
