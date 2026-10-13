package com.studyanchor.focus.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Domain rules for FR-10 (focus progress and streak). Plain Java, no Kafka. */
class FocusProgressTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);
    private static final long MIN_25 = 25 * 60;

    private final Map<LocalDate, Long> seconds = new HashMap<>();

    private FocusProgress progress() {
        return FocusProgress.of(TODAY, seconds);
    }

    private void focus(int daysAgo, long secs) {
        seconds.put(TODAY.minusDays(daysAgo), secs);
    }

    @Test
    void FR10_minutesRoundDown() {
        assertThat(new DailyFocus(TODAY, 59).minutes()).isZero();
        assertThat(new DailyFocus(TODAY, 1559).minutes()).isEqualTo(25);
    }

    @Test
    void FR10_last7DaysEndTodayAndFillMissingWithZero() {
        focus(0, 600);
        focus(3, 1200);

        var days = progress().last7Days();

        assertThat(days).hasSize(7);
        assertThat(days.get(0).day()).isEqualTo(TODAY.minusDays(6));
        assertThat(days.get(6)).isEqualTo(new DailyFocus(TODAY, 600));
        assertThat(days.get(3)).isEqualTo(new DailyFocus(TODAY.minusDays(3), 1200));
        assertThat(days.get(5).seconds()).isZero();
        assertThat(progress().today()).isEqualTo(new DailyFocus(TODAY, 600));
    }

    @Test
    void FR10_streakCountsConsecutiveDaysWithAtLeast25Minutes() {
        focus(0, MIN_25);
        focus(1, 3000);
        focus(2, MIN_25);
        focus(4, 9000); // day 3 is missing, so the streak stops at 3

        assertThat(progress().streak()).isEqualTo(3);
    }

    @Test
    void FR10_24m59sDoesNotCountTowardStreak() {
        focus(0, MIN_25 - 1);
        focus(1, MIN_25 - 1);

        assertThat(new DailyFocus(TODAY, MIN_25 - 1).meetsStreakThreshold()).isFalse();
        assertThat(progress().streak()).isZero();
    }

    @Test
    void FR10_streakCountsFromYesterdayWhenTodayBelowThreshold() {
        focus(0, 300);
        focus(1, MIN_25);
        focus(2, MIN_25);

        assertThat(progress().streak()).isEqualTo(2);
    }

    @Test
    void FR10_streakIsZeroWhenTodayAndYesterdayBelowThreshold() {
        focus(2, MIN_25);
        focus(3, MIN_25);

        assertThat(progress().streak()).isZero();
    }

    @Test
    void FR10_streakHasNoFixedCap() {
        for (int d = 0; d < 1000; d++) {
            focus(d, MIN_25);
        }

        assertThat(progress().streak()).isEqualTo(1000);
    }
}
