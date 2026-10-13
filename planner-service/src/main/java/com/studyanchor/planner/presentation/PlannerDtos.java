package com.studyanchor.planner.presentation;

import com.studyanchor.planner.domain.AvailabilitySlot;
import com.studyanchor.planner.domain.Course;
import com.studyanchor.planner.domain.Deadline;
import com.studyanchor.planner.domain.Goal;
import com.studyanchor.planner.domain.Reminder;
import com.studyanchor.planner.domain.StudyCalendar;
import com.studyanchor.planner.domain.StudyPlan;
import com.studyanchor.planner.domain.TimeBlock;
import com.studyanchor.planner.domain.Topic;
import com.studyanchor.planner.domain.ValueStatement;
import com.studyanchor.planner.domain.WeeklyAvailability;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Implements FR-01, FR-03, FR-04, FR-11, FR-12. The JSON shapes of the Planner API: requests in, responses out. No logic. */
final class PlannerDtos {

    private PlannerDtos() {
    }

    // ---- requests ----

    record CreateCourseRequest(String code, String name) {
    }

    record CreateTopicRequest(String name) {
    }

    /** {@code dueAt} is Sydney local time, e.g. "2026-10-30T23:59" (FR-01 AC8). */
    record CreateDeadlineRequest(String title, LocalDateTime dueAt, Integer weight, List<UUID> topicIds) {
    }

    /** Times are "HH:mm" Sydney wall-clock times (FR-03 AC5). */
    record AddSlotRequest(DayOfWeek day, LocalTime start, LocalTime end) {
    }

    record CreateGoalRequest(String title, String why, LocalDate targetDate) {
    }

    record CreateValueRequest(String text) {
    }

    /** FR-04 AC8: {@code replace} (default false) confirms replacing an existing plan for the week. */
    record GeneratePlanRequest(LocalDate weekStart, boolean replace) {
    }

    // ---- responses ----

    /**
     * {@code materialCount}: materials Tutor reported for the topic (FR-02 AC8); {@code understandingLevel}: the
     * latest level Tutor recorded, null until then (FR-07 AC14).
     */
    record TopicResponse(UUID id, String name, int materialCount, Integer understandingLevel) {

        static TopicResponse from(Topic t) {
            return new TopicResponse(t.id(), t.name(), t.materialCount(), t.understandingLevel());
        }
    }

    /** FR-01 AC8: the due date-time is shown back in Sydney time. */
    record DeadlineResponse(UUID id, String title, String dueAt, int weight, List<UUID> topicIds) {

        static DeadlineResponse from(Deadline d) {
            return new DeadlineResponse(d.id(), d.title(), StudyCalendar.toSydney(d.dueAt()).toString(),
                    d.weight().value(), d.topicIds().stream().sorted().toList());
        }
    }

    record CourseResponse(UUID id, String code, String name, List<TopicResponse> topics,
                          List<DeadlineResponse> deadlines) {

        static CourseResponse from(Course c) {
            return new CourseResponse(c.id(), c.code().value(), c.name(),
                    c.topics().stream().sorted(Comparator.comparing(Topic::name, String.CASE_INSENSITIVE_ORDER))
                            .map(TopicResponse::from).toList(),
                    c.deadlines().stream().sorted(Comparator.comparing(Deadline::dueAt))
                            .map(DeadlineResponse::from).toList());
        }
    }

    record SlotResponse(UUID id, DayOfWeek day, String start, String end) {

        static SlotResponse from(AvailabilitySlot s) {
            return new SlotResponse(s.id(), s.day(), s.start().toString(), s.end().toString());
        }
    }

    record AvailabilityResponse(String userId, List<SlotResponse> slots) {

        static AvailabilityResponse from(String userId, WeeklyAvailability a) {
            return new AvailabilityResponse(userId, a.slots().stream()
                    .sorted(Comparator.comparing(AvailabilitySlot::day).thenComparing(AvailabilitySlot::start))
                    .map(SlotResponse::from).toList());
        }
    }

    record GoalResponse(UUID id, String title, String why, LocalDate targetDate, Instant achievedAt) {

        static GoalResponse from(Goal g) {
            return new GoalResponse(g.id(), g.title(), g.why(), g.targetDate(), g.achievedAt());
        }
    }

    record ValueResponse(UUID id, String text) {

        static ValueResponse from(ValueStatement v) {
            return new ValueResponse(v.id(), v.text());
        }
    }

    /** FR-04 AC5, AC14: times are Sydney time. */
    record BlockResponse(UUID id, LocalDate date, DayOfWeek day, String start, String end, long minutes,
                         UUID topicId, String topicName, String courseCode, String rationale,
                         TimeBlock.Status status) {

        static BlockResponse from(TimeBlock b) {
            return new BlockResponse(b.id(), b.date(), b.date().getDayOfWeek(), b.start().toString(),
                    b.end().toString(), b.minutes(), b.topicId(), b.topicName(), b.courseCode(), b.rationale(),
                    b.status());
        }
    }

    /** FR-04 AC6, AC11: {@code shortfall} is null unless the agent reported too little time. */
    record PlanResponse(UUID id, LocalDate weekStart, String summary, String shortfall, long totalMinutes,
                        List<BlockResponse> blocks) {

        static PlanResponse from(StudyPlan p) {
            return new PlanResponse(p.id(), p.weekStart(), p.summary(), p.shortfall(),
                    p.blocks().stream().mapToLong(TimeBlock::minutes).sum(),
                    p.blocks().stream().map(BlockResponse::from).toList());
        }
    }

    record ReminderResponse(UUID id, Reminder.Kind kind, String message, Instant createdAt, Instant readAt) {

        static ReminderResponse from(Reminder r) {
            return new ReminderResponse(r.id(), r.kind(), r.message(), r.createdAt(), r.readAt());
        }
    }
}
