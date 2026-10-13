package com.studyanchor.social.domain;

import java.util.Optional;
import java.util.UUID;

/** Implements FR-08. Repository for the {@link StudyRoom} aggregate; implemented in infrastructure. */
public interface StudyRoomRepository {

    StudyRoom save(StudyRoom room);

    Optional<StudyRoom> findById(UUID id);
}
