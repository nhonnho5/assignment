package com.studyanchor.planner.domain;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
    void FR12_missedBlockReminderNamesBlockAndQuotesGoal() {
        Course course = Course.create("alice", "CSCI318", "Software Engineering");
        WeeklyAvailability availability = WeeklyAvailability.emptyFor("alice");
        availability.add(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        StudyPlan plan = StudyPlan.create("alice", LocalDate.of(2026, 10, 19), NOW);
        TimeBlock block = plan.addBlock(course, course.addTopic("Kafka", NOW), LocalDate.of(2026, 10, 19),
                LocalTime.of(9, 0), LocalTime.of(9, 50), "Deadline soon.", availability, NOW);

        Reminder reminder = Reminder.forMissedBlock("alice", block, List.of("\"Small steps\""), new Random(7), NOW);

        assertThat(reminder.kind()).isEqualTo(Reminder.Kind.MISSED_BLOCK);
        assertThat(reminder.message()).contains("Kafka (CSCI318)").contains("Mon 19 Oct, 09:00–09:50")
                .contains("\"Small steps\"");
        assertThat(reminder.triggerKey()).isEqualTo(Reminder.missedBlockKey(block.id()));
    }

    @Test
    void FR12_markingReadTwiceIsConflict() {
        Reminder reminder = spike(List.of());

        reminder.markRead(NOW);

        assertThat(reminder.readAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> reminder.markRead(NOW)).isInstanceOf(ConflictException.class);
    }
}
