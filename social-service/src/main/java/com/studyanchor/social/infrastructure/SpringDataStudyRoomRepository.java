package com.studyanchor.social.infrastructure;

import com.studyanchor.social.domain.StudyRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data provides save and findById for study rooms. */
interface SpringDataStudyRoomRepository extends JpaRepository<StudyRoom, UUID> {
}
