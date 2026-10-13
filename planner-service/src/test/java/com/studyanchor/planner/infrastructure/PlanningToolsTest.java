package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.Course;
import com.studyanchor.planner.domain.StudyCalendar;
import com.studyanchor.planner.domain.StudyPlan;
import com.studyanchor.planner.domain.Topic;
import com.studyanchor.planner.domain.WeeklyAvailability;
import com.studyanchor.planner.infrastructure.PlanningTools.BlockProposal;
import com.studyanchor.planner.infrastructure.PlanningTools.FocusStats.Day;
import com.studyanchor.planner.infrastructure.PlanningTools.FocusStats.Progress;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** FR-04 agent tool rules, called directly as the LLM would call them — without the LLM (CLAUDE.md). */
class PlanningToolsTest {

    /** Wednesday 14 Oct 2026, 14:00 in Sydney. */
    private static final Instant NOW = Instant.parse("2026-10-14T03:00:00Z");
    private static final LocalDate NEXT_MONDAY = LocalDate.of(2026, 10, 19);

    private final Course course = Course.create("alice", "CSCI318", "Software Engineering");
    private final Topic kafka = course.addTopic("Kafka", NOW);
    private final Topic streams = course.addTopic("Streams", NOW);
    private final WeeklyAvailability availability = WeeklyAvailability.emptyFor("alice");
    private final StudyPlan plan = StudyPlan.create("alice", NEXT_MONDAY, NOW);
    private final Progress progress = new Progress(new Day(LocalDate.of(2026, 10, 14), "30m 0s", 1800),
            List.of(new Day(LocalDate.of(2026, 10, 13), "45m 0s", 2700)), 2);

    {
        availability.add(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        course.addDeadline("Assignment 2", StudyCalendar.toSydney(NOW).plusDays(3), 30, Set.of(streams.id()), NOW);
    }

    private PlanningTools tools(PlanningTools.FocusStats focus) {
        return new PlanningTools(plan, List.of(course), availability, focus, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private final PlanningTools tools = tools(userId -> progress);

    private BlockProposal block(Topic topic, String date, String start, String end) {
        return new BlockProposal(topic.id().toString(), date, start, end, "The deadline is close.");
    }

    @Test
    void FR04_invalidBlocksAreRefusedWithReasonAndValidOnesKept() {
        String result = tools.addBlocks(List.of(
                block(kafka, "2026-10-19", "09:00", "10:00"),
                block(kafka, "2026-10-19", "10:05", "11:00"),
                block(streams, "2026-10-19", "11:30", "12:30"),
                block(streams, "Monday", "10:10", "11:00"),
                block(streams, "2026-10-19", "10:10", "11:41")));

        assertThat(result).contains("Added 1 block(s)")
                .contains("REFUSED block 2").contains("10 minutes")
                .contains("REFUSED block 3").contains("availability slot")
                .contains("REFUSED block 4").contains("yyyy-MM-dd")
                .contains("REFUSED block 5").contains("25–90");
        assertThat(plan.blocks()).hasSize(1);
    }

    @Test
    void FR04_unknownTopicIsRefused() {
        String result = tools.addBlocks(List.of(
                new BlockProposal("8b1a3c55-0000-4000-8000-000000000000", "2026-10-19", "09:00", "10:00", "Why."),
                new BlockProposal("Kafka", "2026-10-19", "09:00", "10:00", "Why.")));

        assertThat(result).contains("REFUSED block 1").contains("REFUSED block 2").contains("not found")
                .contains("Added 0 block(s)");
        assertThat(plan.blocks()).isEmpty();
    }

    @Test
    void FR04_readToolsShowDeadlinesSlotsAndPriority() {
        assertThat(tools.getTopics()).containsSubsequence("1. Streams", "2. Kafka").contains(streams.id().toString())
                .contains("understanding unknown");
        assertThat(tools.getDeadlines()).contains("Assignment 2").contains("30 %").contains("Streams");
        assertThat(tools.getAvailability()).contains("MONDAY 2026-10-19: 09:00–12:00")
                .contains("TUESDAY 2026-10-20: none");
        assertThat(tools.getFocusStats()).contains("Today: 30m 0s").contains("2026-10-13: 45m 0s")
                .contains("Streak: 2 day(s)");
    }

    @Test
    void FR04_focusStatsUnavailableDoesNotStopPlanning() {
        PlanningTools offline = tools(userId -> {
            throw new IllegalStateException("Connection refused");
        });

        assertThat(offline.getFocusStats()).contains("unavailable");
    }

    @Test
    void FR04_shortfallIsRecorded() {
        assertThat(tools.reportInsufficientTime("Streams needs 3 more hours.")).startsWith("REFUSED")
                .contains("free time first");
        tools.addBlocks(List.of(block(streams, "2026-10-19", "09:00", "10:30")));

        assertThat(tools.reportInsufficientTime(" ")).startsWith("REFUSED");
        assertThat(tools.reportInsufficientTime("Streams needs 3 more hours.")).startsWith("Recorded");

        assertThat(plan.shortfall()).isEqualTo("Streams needs 3 more hours.");
    }

    @Test
    void FR04_addBlocksTakesAListOfBlocksInOneCall() {
        ToolSpecification spec = ToolSpecifications.toolSpecificationsFrom(PlanningTools.class).stream()
                .filter(s -> s.name().equals("addBlocks")).findFirst().orElseThrow();

        JsonArraySchema blocks = (JsonArraySchema) spec.parameters().properties().get("blocks");
        assertThat(((JsonObjectSchema) blocks.items()).properties())
                .containsOnlyKeys("topicId", "date", "start", "end", "rationale");
    }
}
