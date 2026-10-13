package com.studyanchor.social.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Domain rules for FR-08 study rooms. Plain Java, no Spring. */
class StudyRoomTest {

    private static final Instant NOW = Instant.parse("2026-10-14T03:00:00Z");

    @Test
    void FR08_roomNameMustBe1To60Chars() {
        assertThatThrownBy(() -> StudyRoom.create(null, "alice", NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> StudyRoom.create("   ", "alice", NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> StudyRoom.create("x".repeat(61), "alice", NOW)).isInstanceOf(DomainException.class);

        assertThat(StudyRoom.create("  CSCI318 crew ", "alice", NOW).name().value()).isEqualTo("CSCI318 crew");
        assertThat(StudyRoom.create("x".repeat(60), "alice", NOW).name().value()).hasSize(60);
    }

    @Test
    void FR08_roomCreatorIsFirstMember() {
        StudyRoom room = StudyRoom.create("Library", "alice", NOW);

        assertThat(room.members()).containsExactly("alice");
        assertThat(room.isMember("alice")).isTrue();
        assertThat(room.createdBy()).isEqualTo("alice");
    }

    @Test
    void FR08_othersCanJoinByRoomId() {
        StudyRoom room = StudyRoom.create("Library", "alice", NOW);

        room.join("bob");

        assertThat(room.members()).containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void FR08_cannotJoinRoomTwice() {
        StudyRoom room = StudyRoom.create("Library", "alice", NOW);
        room.join("bob");

        assertThatThrownBy(() -> room.join("bob")).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> room.join("alice")).isInstanceOf(ConflictException.class);
    }
}
