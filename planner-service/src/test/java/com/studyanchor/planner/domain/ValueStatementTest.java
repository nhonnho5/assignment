package com.studyanchor.planner.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Domain rules for FR-11 value statements. Plain Java, no Spring. */
class ValueStatementTest {

    private static final Instant NOW = Instant.parse("2026-10-14T03:00:00Z");

    @Test
    void FR11_valueStatementMax280Characters() {
        assertThatThrownBy(() -> ValueStatement.create("alice", "  ", NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> ValueStatement.create("alice", "x".repeat(281), NOW))
                .isInstanceOf(DomainException.class);

        assertThat(ValueStatement.create("alice", " " + "x".repeat(280) + " ", NOW).text()).hasSize(280);
    }
}
