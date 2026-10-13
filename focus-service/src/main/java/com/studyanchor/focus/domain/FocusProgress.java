package com.studyanchor.focus.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Implements FR-10. Value object: a student's focus progress as seen on {@code today}
 * (a Sydney day), built from total focus seconds per day. Days with no entry count as 0.
 */
public final class FocusProgress {

    private final LocalDate today;
    private final Map<LocalDate, Long> secondsPerDay;

    private FocusProgress(LocalDate today, Map<LocalDate, Long> secondsPerDay) {
        this.today = today;
        this.secondsPerDay = Map.copyOf(secondsPerDay);
    }

    public static FocusProgress of(LocalDate today, Map<LocalDate, Long> secondsPerDay) {
        if (today == null || secondsPerDay == null) {
            throw new DomainException("today and secondsPerDay are required");
        }
        return new FocusProgress(today, secondsPerDay);
    }

    /** Focus on any day; 0 seconds if nothing was recorded. */
    public DailyFocus on(LocalDate day) {
        return new DailyFocus(day, secondsPerDay.getOrDefault(day, 0L));
    }

    /** FR-10 AC1. */
    public DailyFocus today() {
        return on(today);
    }

    /** FR-10 AC1: seven days ending today, oldest first. */
    public List<DailyFocus> last7Days() {
        return IntStream.rangeClosed(0, 6)
                .mapToObj(i -> on(today.minusDays(6 - i)))
                .toList();
    }

    /**
     * FR-10 AC2, AC7: number of consecutive days with at least 25 focus minutes, counting back
     * from today if today already qualifies, otherwise from yesterday. No fixed cap.
     */
    public int streak() {
        LocalDate day = on(today).meetsStreakThreshold() ? today : today.minusDays(1);
        int streak = 0;
        while (on(day).meetsStreakThreshold()) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }
}
