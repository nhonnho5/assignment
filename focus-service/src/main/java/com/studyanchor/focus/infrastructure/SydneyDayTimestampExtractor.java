package com.studyanchor.focus.infrastructure;

import com.studyanchor.focus.domain.StudyCalendar;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Implements FR-10 AC4, AC5. Kafka Streams windows are aligned to UTC midnight. To get windows aligned to
 * Sydney midnight, the event time is the session's {@code startedAt} re-read as if Sydney local time were UTC
 * (e.g. 00:30 Sydney becomes 00:30 UTC). A 1-day window then covers exactly one Sydney calendar day,
 * including 23- and 25-hour days at daylight-saving changes.
 */
class SydneyDayTimestampExtractor implements TimestampExtractor {

    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        if (record.value() instanceof FocusSessionMessage message && message.startedAt() != null) {
            return toSydneyTimeline(message.startedAt());
        }
        return record.timestamp();
    }

    static long toSydneyTimeline(Instant instant) {
        return instant.atZone(StudyCalendar.ZONE).toLocalDateTime().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /** Start of the window that holds this Sydney day, on the shifted timeline. */
    static Instant windowStartOf(LocalDate sydneyDay) {
        return sydneyDay.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    /** Inverse of {@link #windowStartOf}. */
    static LocalDate sydneyDayOf(long windowStartMillis) {
        return Instant.ofEpochMilli(windowStartMillis).atZone(ZoneOffset.UTC).toLocalDate();
    }
}
