package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.AvailabilitySlot;
import com.studyanchor.planner.domain.Course;
import com.studyanchor.planner.domain.Deadline;
import com.studyanchor.planner.domain.DomainException;
import com.studyanchor.planner.domain.NotFoundException;
import com.studyanchor.planner.domain.StudyCalendar;
import com.studyanchor.planner.domain.StudyPlan;
import com.studyanchor.planner.domain.Topic;
import com.studyanchor.planner.domain.TopicPriority;
import com.studyanchor.planner.domain.TopicPriority.Ranked;
import com.studyanchor.planner.domain.WeeklyAvailability;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.output.structured.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implements FR-04 AC2–AC7, AC11. The planning agent's tools for one plan. The read tools give the agent the
 * student's data; the write tools change the plan only through the {@link StudyPlan} aggregate, which holds the
 * block rules. A broken rule becomes a "REFUSED: …" answer the LLM reads, so it can fix the block (AC7).
 */
class PlanningTools {

    private static final Logger log = LoggerFactory.getLogger(PlanningTools.class);

    /** AC2: the student's recent focus, from the Focus service ({@code GET /progress/{userId}}). */
    interface FocusStats {

        record Day(LocalDate date, String focus, long seconds) {
        }

        record Progress(Day today, List<Day> last7Days, int streakDays) {
        }

        Progress progressOf(String userId);
    }

    /** One block the agent wants to add; plain strings, so a badly formatted value is refused, not a crash. */
    record BlockProposal(
            @Description("topic ID from getTopics") String topicId,
            @Description("date as yyyy-MM-dd, inside the plan's week") String date,
            @Description("start time as HH:mm, Sydney time") String start,
            @Description("end time as HH:mm, Sydney time") String end,
            @Description("one sentence: why this topic now") String rationale) {
    }

    private final StudyPlan plan;
    private final List<Course> courses;
    private final WeeklyAvailability availability;
    private final FocusStats focus;
    private final Clock clock;

    PlanningTools(StudyPlan plan, List<Course> courses, WeeklyAvailability availability, FocusStats focus,
                  Clock clock) {
        this.plan = plan;
        this.courses = courses;
        this.availability = availability;
        this.focus = focus;
        this.clock = clock;
    }

    @Tool("Lists the student's deadlines that are still due: course, title, due date-time (Sydney), weight and topics.")
    String getDeadlines() {
        Instant now = clock.instant();
        String result = courses.stream()
                .flatMap(c -> c.deadlines().stream().filter(d -> d.dueAt().isAfter(now))
                        .map(d -> Map.entry(d.dueAt(), describe(c, d))))
                .sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue)
                .collect(Collectors.joining("\n"));
        return logged("getDeadlines", result.isEmpty() ? "No deadlines are due." : result);
    }

    private static String describe(Course course, Deadline deadline) {
        return course.code().value() + " \"" + deadline.title() + "\" due " + StudyCalendar.toSydney(deadline.dueAt())
                + ", weight " + deadline.weight().value() + " %, topics: " + course.topics().stream()
                .filter(t -> deadline.topicIds().contains(t.id())).map(Topic::name).collect(Collectors.joining(", "));
    }

    @Tool("Lists the student's free study time in the plan's week: availability slots on real dates (Sydney time)."
            + " Every block must fit inside one slot.")
    String getAvailability() {
        String result = IntStream.range(0, 7).mapToObj(plan.weekStart()::plusDays)
                .map(date -> date.getDayOfWeek() + " " + date + ": " + slotsOn(date))
                .collect(Collectors.joining("\n"));
        return logged("getAvailability", result);
    }

    @Tool("Lists the student's topics, highest priority first, with topic ID, course, understanding level (1–5) and"
            + " priority score. Give higher-ranked topics more and earlier blocks.")
    String getTopics() {
        List<Ranked> ranked = TopicPriority.rank(courses, clock.instant());
        String result = IntStream.range(0, ranked.size()).mapToObj(i -> {
            Ranked r = ranked.get(i);
            Integer level = r.topic().understandingLevel();
            return (i + 1) + ". " + r.topic().name() + " (" + r.course().code().value() + "), id "
                    + r.topic().id() + ", understanding " + (level == null ? "unknown" : level + "/5")
                    + ", priority " + Math.round(r.score() * 10) / 10.0;
        }).collect(Collectors.joining("\n"));
        return logged("getTopics", result);
    }

    @Tool("Returns the student's focus time for today and the last 7 days and their streak. Use it to pick block"
            + " lengths the student can manage.")
    String getFocusStats() {
        String result;
        try {
            FocusStats.Progress p = focus.progressOf(plan.userId());
            result = "Today: " + p.today().focus() + ". Last 7 days: " + p.last7Days().stream()
                    .map(d -> d.date() + ": " + d.focus()).collect(Collectors.joining(", "))
                    + ". Streak: " + p.streakDays() + " day(s).";
        } catch (RuntimeException e) {
            result = "Focus statistics are unavailable right now (" + e.getMessage() + "); plan without them.";
        }
        return logged("getFocusStats", result);
    }

    @Tool("Adds time blocks to the plan in one call. Valid blocks are kept; each invalid one comes back as"
            + " REFUSED with the reason, so fix and resend only those.")
    String addBlocks(@P("the blocks to add") List<BlockProposal> blocks) {
        List<String> refused = new ArrayList<>();
        int added = 0;
        for (int i = 0; i < blocks.size(); i++) {
            BlockProposal b = blocks.get(i);
            try {
                Ranked topic = topic(b.topicId());
                LocalDate date = parse(() -> LocalDate.parse(b.date()), "date must be yyyy-MM-dd");
                LocalTime start = parse(() -> LocalTime.parse(b.start()), "start must be HH:mm");
                LocalTime end = parse(() -> LocalTime.parse(b.end()), "end must be HH:mm");
                plan.addBlock(topic.course(), topic.topic(), date, start, end, b.rationale(), availability,
                        clock.instant());
                added++;
            } catch (DomainException e) {
                refused.add("REFUSED block " + (i + 1) + " (" + b.date() + " " + b.start() + "–" + b.end() + "): "
                        + e.getMessage());
            }
        }
        String result = "Added " + added + " block(s); the plan now has " + plan.blocks().size() + " block(s)."
                + (refused.isEmpty() ? "" : "\n" + String.join("\n", refused));
        return logged("addBlocks(" + blocks.size() + ")", result);
    }

    @Tool("Records that the available time is not enough for what the student needs to study, and why. Call it"
            + " instead of overbooking.")
    String reportInsufficientTime(@P("a short explanation for the student") String reason) {
        String result;
        try {
            plan.reportShortfall(reason, availability, clock.instant());
            result = "Recorded. Mention it in your summary.";
        } catch (DomainException e) {
            result = "REFUSED: " + e.getMessage();
        }
        return logged("reportInsufficientTime", result);
    }

    private String slotsOn(LocalDate date) {
        String slots = availability.slots().stream().filter(s -> s.day() == date.getDayOfWeek())
                .sorted(Comparator.comparing(AvailabilitySlot::start))
                .map(s -> s.start() + "–" + s.end()).collect(Collectors.joining(", "));
        return slots.isEmpty() ? "none" : slots;
    }

    private Ranked topic(String topicId) {
        return TopicPriority.rank(courses, clock.instant()).stream()
                .filter(r -> r.topic().id().toString().equals(topicId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Topic " + topicId + " not found; use an id from getTopics"));
    }

    private static <T> T parse(Supplier<T> parser, String error) {
        try {
            return parser.get();
        } catch (DateTimeException | NullPointerException e) {
            throw new DomainException(error);
        }
    }

    /** Logs the outcome as demo evidence of which tools ran (docs/09-conventions.md). */
    private String logged(String tool, String result) {
        log.info("Tool {} in plan {} → {}", tool, plan.id(), result.lines().findFirst().orElse(""));
        return result;
    }
}
