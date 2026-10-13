package com.studyanchor.planner.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Domain rules for FR-12 reminders. Plain Java, no Spring. */
class ReminderTest {

    private static final Instant WINDOW_START = Instant.parse("2026-10-14T03:00:00Z");
    private static final Instant NOW = WINDOW_START.plusSeconds(600);

    private static Reminder spike(List<String> quotes) {
        return Reminder.forDistractionSpike("alice", 3, WINDOW_START, quotes, new Random(7), NOW);
    }

    @Test
    void FR12_reminderQuotesOneOfTheStudentsWords() {
        Reminder reminder = spike(List.of("\"Small steps\"", "your goal \"Pass\": \"Graduate\""));

        assertThat(reminder.kind()).isEqualTo(Reminder.Kind.DISTRACTION_SPIKE);
        assertThat(reminder.message()).contains("3 distractions")
                .containsAnyOf("\"Small steps\"", "your goal \"Pass\": \"Graduate\"");
        assertThat(reminder.createdAt()).isEqualTo(NOW);
        assertThat(reminder.isRead()).isFalse();
    }

    @Test
    void FR12_genericReminderWhenNothingToQuote() {
        assertThat(spike(List.of()).message()).contains("3 distractions").contains("Tip: add a goal");
    }

    @Test
    void FR12_markingReadTwiceIsConflict() {
        Reminder reminder = spike(List.of());

        reminder.markRead(NOW);

        assertThat(reminder.readAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> reminder.markRead(NOW)).isInstanceOf(ConflictException.class);
    }
}
