package com.studyanchor.planner.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Domain rules for FR-04 AC4 topic priority. Plain Java, no Spring. */
class TopicPriorityTest {

    private static final Instant NOW = Instant.parse("2026-10-14T03:00:00Z");

    private final Course course = Course.create("alice", "CSCI318", "Software Engineering");

    private void deadline(Topic topic, int daysAway, int weight) {
        LocalDateTime due = StudyCalendar.toSydney(NOW).plusDays(daysAway);
        course.addDeadline("Assignment " + topic.name(), due, weight, Set.of(topic.id()), NOW);
    }

    private List<String> ranking(Instant at) {
        return TopicPriority.rank(List.of(course), at).stream().map(r -> r.topic().name()).toList();
    }

    @Test
    void FR04_nearerHeavierDeadlineRanksFirst() {
        Topic near = course.addTopic("Near", NOW);
        Topic far = course.addTopic("Far", NOW);
        Topic heavy = course.addTopic("Heavy", NOW);
        deadline(near, 2, 20);
        deadline(far, 10, 20);
        deadline(heavy, 2, 40);

        assertThat(ranking(NOW)).containsExactly("Heavy", "Near", "Far");
    }

    @Test
    void FR04_lowerUnderstandingRanksFirstAndUnknownCountsAsThree() {
        Topic known = course.addTopic("Known", NOW);
        Topic weak = course.addTopic("Weak", NOW);
        Topic unknown = course.addTopic("Unknown", NOW);
        course.addDeadline("Exam", StudyCalendar.toSydney(NOW).plusDays(5), 50,
                Set.of(known.id(), weak.id(), unknown.id()), NOW);
        course.recordUnderstanding(known.id(), 5, NOW);
        course.recordUnderstanding(weak.id(), 1, NOW);

        assertThat(ranking(NOW)).containsExactly("Weak", "Unknown", "Known");
    }

    @Test
    void FR04_topicsWithoutDeadlineStillDueScoreZero() {
        Topic past = course.addTopic("Past", NOW);
        course.addTopic("None", NOW);
        deadline(past, 1, 30);

        List<TopicPriority.Ranked> later = TopicPriority.rank(List.of(course), NOW.plusSeconds(3 * 86_400));

        assertThat(later).allSatisfy(r -> assertThat(r.score()).isZero());
        assertThat(TopicPriority.rank(List.of(course), NOW).getFirst().topic().name()).isEqualTo("Past");
    }
}
