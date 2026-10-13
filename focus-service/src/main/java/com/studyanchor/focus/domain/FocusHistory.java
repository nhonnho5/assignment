package com.studyanchor.focus.domain;

import java.time.LocalDate;
import java.util.Map;

/**
 * Implements FR-10. Port: total focus seconds per Sydney day for one student, for every retained
 * day up to and including {@code until}. Implemented in infrastructure by the Kafka Streams state store.
 */
public interface FocusHistory {

    Map<LocalDate, Long> secondsPerDay(String userId, LocalDate until);
}
