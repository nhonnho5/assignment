package com.studyanchor.focus.service;

import com.studyanchor.focus.domain.FocusHistory;
import com.studyanchor.focus.domain.FocusProgress;
import com.studyanchor.focus.domain.StudyCalendar;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/** Implements FR-10. Application service: reads a student's daily focus totals and builds their progress. */
@Service
public class FocusProgressService {

    private final FocusHistory history;
    private final Clock clock;

    public FocusProgressService(FocusHistory history, Clock clock) {
        this.history = history;
        this.clock = clock;
    }

    /** FR-10 AC1, AC2: progress as of today in Sydney. */
    public FocusProgress progressOf(String userId) {
        LocalDate today = StudyCalendar.dayOf(clock.instant());
        return FocusProgress.of(today, history.secondsPerDay(userId, today));
    }
}
