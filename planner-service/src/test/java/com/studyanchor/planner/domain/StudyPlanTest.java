package com.studyanchor.planner.domain;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Domain rules for FR-04 study plans and FR-12 AC12 block status. Plain Java, no Spring. */
class StudyPlanTest {

    /** Wednesday 14 Oct 2026, 14:00 in Sydney. */
    private static final Instant NOW = Instant.parse("2026-10-14T03:00:00Z");
    private static final LocalDate NEXT_MONDAY = LocalDate.of(2026, 10, 19);
    private static final LocalDate THIS_MONDAY = LocalDate.of(2026, 10, 12);

    private final WeeklyAvailability availability = WeeklyAvailability.emptyFor("alice");
    private final Course course = Course.create("alice", "CSCI318", "Software Engineering");
    private final Topic kafka = course.addTopic("Kafka", NOW);
    private final StudyPlan plan = StudyPlan.create("alice", NEXT_MONDAY, NOW);

    {
        availability.add(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        availability.add(DayOfWeek.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(16, 0));
    }

    private TimeBlock add(StudyPlan plan, LocalDate date, String start, String end) {
        return plan.addBlock(course, kafka, date, LocalTime.parse(start), LocalTime.parse(end),
                "The Kafka deadline is close.", availability, NOW);
    }

    private TimeBlock add(LocalDate date, String start, String end) {
        return add(plan, date, start, end);
    }

    @Test
    void FR04_weekStartMustBeMonday() {
        assertThatThrownBy(() -> StudyPlan.create("alice", NEXT_MONDAY.plusDays(1), NOW))
                .isInstanceOf(DomainException.class).hasMessageContaining("Monday");
        assertThatThrownBy(() -> StudyPlan.create("alice", null, NOW)).isInstanceOf(DomainException.class);
    }

    @Test
    void FR04_weekAlreadyOverIsRejected() {
        assertThatThrownBy(() -> StudyPlan.create("alice", THIS_MONDAY.minusWeeks(1), NOW))
                .isInstanceOf(DomainException.class).hasMessageContaining("over");
        assertThat(StudyPlan.create("alice", THIS_MONDAY, NOW).weekStart()).isEqualTo(THIS_MONDAY);
    }

    @Test
    void FR04_blockMustFitInOneAvailabilitySlot() {
        availability.add(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(13, 0));

        assertThatThrownBy(() -> add(NEXT_MONDAY, "11:30", "12:30"))
                .isInstanceOf(DomainException.class).hasMessageContaining("availability slot");
        assertThatThrownBy(() -> add(NEXT_MONDAY.plusDays(1), "09:00", "10:00"))
                .isInstanceOf(DomainException.class).hasMessageContaining("availability slot");

        TimeBlock block = add(NEXT_MONDAY, "09:00", "10:00");
        assertThat(block.date()).isEqualTo(NEXT_MONDAY);
        assertThat(block.start()).isEqualTo(LocalTime.of(9, 0));
        assertThat(block.topicName()).isEqualTo("Kafka");
        assertThat(block.courseCode()).isEqualTo("CSCI318");
        assertThat(block.status()).isEqualTo(TimeBlock.Status.PLANNED);
    }

    @Test
    void FR04_blockMustBe25To90Minutes() {
        assertThatThrownBy(() -> add(NEXT_MONDAY, "09:00", "09:24"))
                .isInstanceOf(DomainException.class).hasMessageContaining("25");
        assertThatThrownBy(() -> add(NEXT_MONDAY, "09:00", "10:31"))
                .isInstanceOf(DomainException.class).hasMessageContaining("90");

        add(NEXT_MONDAY, "09:00", "09:25");
        add(NEXT_MONDAY, "10:00", "11:30");
        assertThat(plan.blocks()).hasSize(2);
    }

    @Test
    void FR04_blocksNeedTenMinutesBetween() {
        add(NEXT_MONDAY, "09:00", "10:00");

        assertThatThrownBy(() -> add(NEXT_MONDAY, "10:09", "11:00"))
                .isInstanceOf(DomainException.class).hasMessageContaining("10 minutes");
        assertThatThrownBy(() -> add(NEXT_MONDAY, "09:30", "10:30")).isInstanceOf(DomainException.class);

        add(NEXT_MONDAY, "10:10", "11:00");
        assertThat(plan.blocks()).hasSize(2);
    }

    @Test
    void FR04_blockMustBeInPlanWeek() {
        assertThatThrownBy(() -> add(NEXT_MONDAY.plusWeeks(1), "09:00", "10:00"))
                .isInstanceOf(DomainException.class).hasMessageContaining("week");
        assertThatThrownBy(() -> add(THIS_MONDAY, "09:00", "10:00"))
                .isInstanceOf(DomainException.class).hasMessageContaining("week");
    }

    @Test
    void FR04_blockCannotStartInThePast() {
        StudyPlan thisWeek = StudyPlan.create("alice", THIS_MONDAY, NOW);
        LocalDate today = LocalDate.of(2026, 10, 14);

        assertThatThrownBy(() -> add(thisWeek, today, "13:30", "14:30"))
                .isInstanceOf(DomainException.class).hasMessageContaining("past");
        add(thisWeek, today, "14:30", "15:00");
        assertThat(thisWeek.blocks()).hasSize(1);
    }

    @Test
    void FR04_blockNeedsRationaleAndWholeMinutes() {
        assertThatThrownBy(() -> plan.addBlock(course, kafka, NEXT_MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0),
                " ", availability, NOW)).isInstanceOf(DomainException.class).hasMessageContaining("rationale");
        assertThatThrownBy(() -> plan.addBlock(course, kafka, NEXT_MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0),
                "x".repeat(201), availability, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> plan.addBlock(course, kafka, NEXT_MONDAY, LocalTime.of(9, 0, 30),
                LocalTime.of(10, 0), "Why.", availability, NOW)).isInstanceOf(DomainException.class);
    }

    @Test
    void FR04_shortfallOnlyAfterUsingTheFreeTime() {
        assertThatThrownBy(() -> plan.reportShortfall("Not enough time.", availability, NOW))
                .isInstanceOf(DomainException.class).hasMessageContaining("free time first");

        add(NEXT_MONDAY, "09:00", "10:00");
        plan.reportShortfall("Not enough time.", availability, NOW);
        assertThat(plan.shortfall()).isEqualTo("Not enough time.");

        StudyPlan noFreeSlotLeft = StudyPlan.create("alice", THIS_MONDAY, Instant.parse("2026-10-14T06:00:00Z"));
        noFreeSlotLeft.reportShortfall("No time left this week.", availability, Instant.parse("2026-10-14T06:00:00Z"));
        assertThat(noFreeSlotLeft.shortfall()).isEqualTo("No time left this week.");
    }

    @Test
    void FR04_startingOverClearsBlocksShortfallAndSummary() {
        add(NEXT_MONDAY, "09:00", "10:00");
        plan.reportShortfall("Not enough time for Streams.", availability, NOW);
        plan.summarise("Here is your week.");

        assertThat(plan.shortfall()).isEqualTo("Not enough time for Streams.");
        assertThat(plan.summary()).isEqualTo("Here is your week.");

        plan.startOver();

        assertThat(plan.blocks()).isEmpty();
        assertThat(plan.shortfall()).isNull();
        assertThat(plan.summary()).isNull();
    }

    @Test
    void FR12_endedSessionCompletesBlockEvenWhenMissed() {
        TimeBlock first = add(NEXT_MONDAY, "09:00", "10:00");
        TimeBlock second = add(NEXT_MONDAY, "10:30", "11:30");

        plan.sessionEnded(first.id());
        plan.markMissed(Instant.parse("2026-10-19T01:00:00Z"));
        plan.sessionEnded(second.id());

        assertThat(first.status()).isEqualTo(TimeBlock.Status.COMPLETED);
        assertThat(second.status()).isEqualTo(TimeBlock.Status.COMPLETED);
    }

    @Test
    void FR12_startedBlockIsNotMissed() {
        TimeBlock block = add(NEXT_MONDAY, "09:00", "10:00");

        plan.sessionStarted(block.id());

        assertThat(plan.markMissed(Instant.parse("2026-10-19T01:00:00Z"))).isEmpty();
        assertThat(block.status()).isEqualTo(TimeBlock.Status.STARTED);
    }

    @Test
    void FR12_blockIsMarkedMissedOnceAfterItEnds() {
        TimeBlock block = add(NEXT_MONDAY, "09:00", "10:00");

        assertThat(plan.markMissed(Instant.parse("2026-10-18T22:59:00Z"))).isEmpty();
        assertThat(plan.markMissed(Instant.parse("2026-10-18T23:00:00Z"))).containsExactly(block);
        assertThat(plan.markMissed(Instant.parse("2026-10-19T01:00:00Z"))).isEmpty();
        assertThat(block.status()).isEqualTo(TimeBlock.Status.MISSED);
    }
}
