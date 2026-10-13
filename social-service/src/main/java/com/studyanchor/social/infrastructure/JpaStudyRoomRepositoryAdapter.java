package com.studyanchor.social.infrastructure;

import com.studyanchor.social.domain.StudyRoom;
import com.studyanchor.social.domain.StudyRoomRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Implements FR-08. Adapter: fulfils the domain repository interface using Spring Data JPA and H2. */
@Repository
class JpaStudyRoomRepositoryAdapter implements StudyRoomRepository {

    private final SpringDataStudyRoomRepository jpa;

    JpaStudyRoomRepositoryAdapter(SpringDataStudyRoomRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public StudyRoom save(StudyRoom room) {
        return jpa.save(room);
    }

    @Override
    public Optional<StudyRoom> findById(UUID id) {
        return jpa.findById(id);
    }
}
