package com.studyanchor.planner.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Domain rules for FR-11 goals. Plain Java, no Spring. */
class GoalTest {

    /** 2026-10-14 09:00 in Sydney, but still 2026-10-13 in UTC. */
    private static final Instant NOW = Instant.parse("2026-10-13T22:00:00Z");

    @Test
    void FR11_goalNeedsTitleAndWhy() {
        assertThatThrownBy(() -> Goal.create("alice", " ", "why", null, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Goal.create("alice", "Pass", null, null, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Goal.create("alice", "x".repeat(101), "why", null, NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Goal.create("alice", "Pass", "x".repeat(501), null, NOW))
                .isInstanceOf(DomainException.class);

        Goal goal = Goal.create("alice", " Pass CSCI318 ", " Graduate on time ", null, NOW);
        assertThat(goal.title()).isEqualTo("Pass CSCI318");
        assertThat(goal.why()).isEqualTo("Graduate on time");
    }

    @Test
    void FR11_targetDateCannotBeBeforeToday() {
        LocalDate todayInSydney = LocalDate.parse("2026-10-14");

        assertThatThrownBy(() -> Goal.create("alice", "Pass", "why", todayInSydney.minusDays(1), NOW))
                .isInstanceOf(DomainException.class);
        assertThat(Goal.create("alice", "Pass", "why", todayInSydney, NOW).targetDate()).isEqualTo(todayInSydney);
    }

    @Test
    void FR11_achievingTwiceIsConflict() {
        Goal goal = Goal.create("alice", "Pass", "why", null, NOW);

        goal.achieve(NOW);

        assertThat(goal.achievedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> goal.achieve(NOW)).isInstanceOf(ConflictException.class);
    }
}
