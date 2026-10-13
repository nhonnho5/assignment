package com.studyanchor.focus.presentation;

import com.studyanchor.focus.domain.DailyFocus;
import com.studyanchor.focus.domain.FocusProgress;

import java.time.LocalDate;
import java.util.List;

/**
 * Implements FR-10. JSON shape of a student's focus progress. Each day shows its focus time as minutes and
 * seconds (e.g. {@code 24m 59s}, same format as FR-05) plus the exact total in seconds.
 */
record ProgressResponse(String userId, Day today, List<Day> last7Days, int streakDays) {

    record Day(LocalDate date, String focus, long seconds) {

        static Day from(DailyFocus daily) {
            return new Day(daily.day(), daily.minutes() + "m " + daily.seconds() % 60 + "s", daily.seconds());
        }
    }

    static ProgressResponse from(String userId, FocusProgress progress) {
        return new ProgressResponse(userId, Day.from(progress.today()),
                progress.last7Days().stream().map(Day::from).toList(), progress.streak());
    }
}
