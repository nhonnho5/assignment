package com.studyanchor.planner.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Implements FR-04 AC4, AC12. Domain service: ranks the student's topics so nearer, heavier deadlines and lower
 * understanding come first. Score = (6 − level, unknown counts as 3) × Σ (weight ÷ days until due, at least 1).
 */
public final class TopicPriority {

    public record Ranked(Course course, Topic topic, double score) {
    }

    private TopicPriority() {
    }

    /** Highest score first; ties by course code, then topic name. */
    public static List<Ranked> rank(List<Course> courses, Instant now) {
        return courses.stream()
                .flatMap(c -> c.topics().stream().map(t -> new Ranked(c, t, score(c, t, now))))
                .sorted(Comparator.comparingDouble(Ranked::score).reversed()
                        .thenComparing(r -> r.course().code().value())
                        .thenComparing(r -> r.topic().name()))
                .toList();
    }

    private static double score(Course course, Topic topic, Instant now) {
        double urgency = course.deadlines().stream()
                .filter(d -> d.topicIds().contains(topic.id()) && d.dueAt().isAfter(now))
                .mapToDouble(d -> d.weight().value() / Math.max(1, Duration.between(now, d.dueAt()).toMinutes() / 1440.0))
                .sum();
        int level = topic.understandingLevel() == null ? 3 : topic.understandingLevel();
        return (6 - level) * urgency;
    }
}
