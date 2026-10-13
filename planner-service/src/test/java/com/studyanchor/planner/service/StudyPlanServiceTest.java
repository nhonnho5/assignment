package com.studyanchor.planner.service;

import com.studyanchor.planner.domain.ConflictException;
import com.studyanchor.planner.domain.Course;
import com.studyanchor.planner.domain.DomainException;
import com.studyanchor.planner.domain.Goal;
import com.studyanchor.planner.domain.PlanningAgent;
import com.studyanchor.planner.domain.Reminder;
import com.studyanchor.planner.domain.StudyPlan;
import com.studyanchor.planner.domain.StudyPlanRepository;
import com.studyanchor.planner.domain.TimeBlock;
import com.studyanchor.planner.domain.WeeklyAvailability;
import com.studyanchor.planner.domain.WeeklyAvailabilityRepository;
import com.studyanchor.planner.service.ReminderServiceTest.InMemoryReminders;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Application-service rules for FR-04 plans and FR-12 AC4/AC12 block tracking, with in-memory fakes. */
class StudyPlanServiceTest {

    /** Wednesday 14 Oct 2026, 14:00 in Sydney. */
    private static final Instant NOW = Instant.parse("2026-10-14T03:00:00Z");
    private static final LocalDate NEXT_MONDAY = LocalDate.of(2026, 10, 19);
    /** Monday 19 Oct, 11:00 in Sydney: the 09:00–10:00 block has ended. */
    private static final Instant AFTER_BLOCK = Instant.parse("2026-10-19T00:00:00Z");

    /** Keeps plans in a map instead of a database. */
    static class InMemoryPlans implements StudyPlanRepository {

        final Map<UUID, StudyPlan> byId = new HashMap<>();

        public StudyPlan save(StudyPlan plan) {
            byId.put(plan.id(), plan);
            return plan;
        }

        public Optional<StudyPlan> findByUserIdAndWeekStart(String userId, LocalDate weekStart) {
            return byId.values().stream()
                    .filter(p -> p.userId().equals(userId) && p.weekStart().equals(weekStart)).findFirst();
        }

        public Optional<StudyPlan> findByBlockId(UUID blockId) {
            return byId.values().stream()
                    .filter(p -> p.blocks().stream().anyMatch(b -> b.id().equals(blockId))).findFirst();
        }

        public List<StudyPlan> findWithUnstartedBlocksEndedBy(Instant now) {
            return List.copyOf(byId.values());
        }
    }

    private final CourseServiceTest.InMemoryCourses courses = new CourseServiceTest.InMemoryCourses();
    private final InMemoryPlans plans = new InMemoryPlans();
    private final InMemoryReminders reminders = new InMemoryReminders();
    private final List<Goal> goals = new ArrayList<>();
    private final WeeklyAvailability availability = WeeklyAvailability.emptyFor("alice");
    private final WeeklyAvailabilityRepository availabilities = new WeeklyAvailabilityRepository() {
        public WeeklyAvailability save(WeeklyAvailability a) {
            return a;
        }

        public Optional<WeeklyAvailability> findByUserId(String userId) {
            return Optional.of(availability).filter(a -> userId.equals("alice"));
        }
    };
    private int agentCalls;
    private String blockStart = "09:00";

    /** Stands in for the LLM: adds one block through the aggregate, as the real agent's tools do. */
    private final PlanningAgent agent = (plan, studentCourses, slots) -> {
        agentCalls++;
        Course course = studentCourses.getFirst();
        plan.addBlock(course, course.topics().getFirst(), NEXT_MONDAY, LocalTime.parse(blockStart),
                LocalTime.parse(blockStart).plusHours(1), "The Kafka deadline is close.", slots, NOW);
        return "  Here is your week.  ";
    };

    private StudyPlanService serviceAt(Instant now) {
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        ReminderService reminderService = new ReminderService(reminders, ReminderServiceTest.fakeGoals(goals),
                ReminderServiceTest.fakeValues(List.of()), clock);
        return new StudyPlanService(plans, courses, availabilities, agent, reminderService, clock);
    }

    private final StudyPlanService service = serviceAt(NOW);

    private void studentHasTopicsAndTime() {
        Course course = Course.create("alice", "CSCI318", "Software Engineering");
        course.addTopic("Kafka", NOW);
        courses.save(course);
        availability.add(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
    }

    private TimeBlock plannedBlock() {
        studentHasTopicsAndTime();
        return service.generate("alice", NEXT_MONDAY, false).blocks().getFirst();
    }

    @Test
    void FR04_generateSavesBlocksAndSummary() {
        studentHasTopicsAndTime();

        StudyPlan plan = service.generate("alice", NEXT_MONDAY, false);

        assertThat(plan.blocks()).hasSize(1);
        assertThat(plan.summary()).isEqualTo("Here is your week.");
        assertThat(service.get("alice", NEXT_MONDAY)).isSameAs(plan);
    }

    @Test
    void FR04_existingPlanIsReplacedOnlyWhenAsked() {
        studentHasTopicsAndTime();
        StudyPlan first = service.generate("alice", NEXT_MONDAY, false);

        blockStart = "10:30";
        assertThatThrownBy(() -> service.generate("alice", NEXT_MONDAY, false))
                .isInstanceOf(ConflictException.class).hasMessageContaining("\"replace\": true");
        assertThat(agentCalls).isEqualTo(1);

        StudyPlan replaced = service.generate("alice", NEXT_MONDAY, true);
        assertThat(replaced.id()).isEqualTo(first.id());
        assertThat(replaced.blocks()).singleElement().satisfies(b -> assertThat(b.start()).isEqualTo(LocalTime.of(10, 30)));
    }

    @Test
    void FR04_noAvailabilityOrNoTopicsIsRejectedBeforeCallingTheAgent() {
        assertThatThrownBy(() -> service.generate("alice", NEXT_MONDAY, false))
                .isInstanceOf(DomainException.class).hasMessageContaining("availability");
        availability.add(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        assertThatThrownBy(() -> service.generate("alice", NEXT_MONDAY, false))
                .isInstanceOf(DomainException.class).hasMessageContaining("topic");

        assertThat(agentCalls).isZero();
    }

    @Test
    void FR12_sessionEventsMarkOnlyTheOwnersBlock() {
        TimeBlock block = plannedBlock();

        service.onFocusSessionStarted("bob", block.id());
        assertThat(block.status()).isEqualTo(TimeBlock.Status.PLANNED);
        service.onFocusSessionStarted("alice", block.id());
        assertThat(block.status()).isEqualTo(TimeBlock.Status.STARTED);
        service.onFocusSessionEnded("alice", UUID.randomUUID());
        service.onFocusSessionEnded("alice", block.id());
        assertThat(block.status()).isEqualTo(TimeBlock.Status.COMPLETED);
    }

    @Test
    void FR12_missedBlockCreatesOneReminderQuotingAGoal() {
        TimeBlock block = plannedBlock();
        goals.add(Goal.create("alice", "Pass CSCI318", "I want to graduate this year", null, NOW));

        serviceAt(AFTER_BLOCK).markMissedBlocks();
        serviceAt(AFTER_BLOCK.plusSeconds(60)).markMissedBlocks();

        assertThat(block.status()).isEqualTo(TimeBlock.Status.MISSED);
        assertThat(reminders.reminders).singleElement().satisfies(r -> {
            assertThat(r.kind()).isEqualTo(Reminder.Kind.MISSED_BLOCK);
            assertThat(r.message()).contains("Kafka (CSCI318)").contains("I want to graduate this year");
        });
    }

    @Test
    void FR12_startedBlockGetsNoMissedReminder() {
        TimeBlock block = plannedBlock();
        service.onFocusSessionStarted("alice", block.id());

        serviceAt(AFTER_BLOCK).markMissedBlocks();

        assertThat(reminders.reminders).isEmpty();
    }
}
