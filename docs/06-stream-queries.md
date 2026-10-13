# 06 — Stream processing queries

> Origin: drafted with AI assistance (Claude Code) from the Kafka Streams topologies; reviewed by the author.

StudyAnchor has three Kafka Streams topologies (Spring Cloud Stream, Kafka Streams binder). Each one is written below as a SQL-like query in ksqlDB style, so the logic is easy to read. **The queries are a description only: the real implementation is the Java code linked under each query**, and ksqlDB is not used.

All topics use `userId` as the message key ([05-events.md](05-events.md)). Each user's events therefore land in the same partition and stay in order, and grouping by user needs no repartitioning.

## Q1 — Focus minutes per Sydney day (FR-10)

```sql
CREATE TABLE focus_progress AS
  SELECT userId,
         WINDOWSTART AS sydneyDay,
         SUM(actualSeconds) AS seconds
  FROM focus_sessions
       WITH (TIMESTAMP = startedAt shifted to the Sydney day)
  WHERE eventType = 'FocusSessionEnded'
  WINDOW TUMBLING (SIZE 1 DAY, GRACE PERIOD 1 DAY, RETENTION 3650 DAYS)
  GROUP BY userId;
```

- **Input:** `focus.sessions`. **State store:** `focus-progress-store`, a windowed key-value store backed by a changelog topic.
- **Event time** is the session's `startedAt`, so a session that ends after midnight still counts for its start day (FR-10 AC5). `SydneyDayTimestampExtractor` shifts the timestamp so the 1-day windows start at Sydney midnight instead of UTC midnight.
- **Grace 1 day:** an ended session that arrives later than that is not counted (AC6).
- **Real-time query:** `GET /progress/{userId}` runs an interactive query that fetches all of the student's stored daily totals. Then `FocusProgress`, a pure domain class with unit tests, computes the streak (AC7).
- **Code:** [`FocusProgressTopology`](../focus-service/src/main/java/com/studyanchor/focus/infrastructure/FocusProgressTopology.java), [`KafkaStreamsFocusHistory`](../focus-service/src/main/java/com/studyanchor/focus/infrastructure/KafkaStreamsFocusHistory.java), [`FocusProgress`](../focus-service/src/main/java/com/studyanchor/focus/domain/FocusProgress.java).

## Q2 — Who is studying now (FR-09)

```sql
CREATE TABLE presence AS
  SELECT userId,
         LATEST_BY_OFFSET(sessionId) AS sessionId,
         LATEST_BY_OFFSET(topicId)   AS topicId,
         LATEST_BY_OFFSET(startedAt) AS startedAt
  FROM focus_sessions
  GROUP BY userId;
  -- FocusSessionStarted upserts the row; FocusSessionEnded deletes it (tombstone)

-- Query time (Social service), friends board:
SELECT p.userId, p.topicId, NOW() - p.startedAt AS elapsed
FROM presence p
JOIN friendships f ON f.friendOf(:viewer) = p.userId
JOIN sharing_settings s ON s.userId = p.userId AND s.sharing = TRUE;

-- Query time, room count (sharing setting ignored: anonymous number):
SELECT COUNT(*) FROM presence WHERE userId IN (members of :roomId);
```

- **Input:** `focus.sessions`. **State store:** `presence-store`, a key-value KTable.
- A `FocusSessionEnded` maps to a `null` value. `toTable` treats that as a tombstone and deletes the user, so the board shows only open sessions.
- **The sharing filter is applied when someone asks,** not in the stream, against Social's own `SharingSetting` table. Turning sharing off therefore works at once (FR-09 AC5).
- **Code:** [`PresenceTopology`](../social-service/src/main/java/com/studyanchor/social/infrastructure/PresenceTopology.java), [`KafkaStreamsPresenceBoard`](../social-service/src/main/java/com/studyanchor/social/infrastructure/KafkaStreamsPresenceBoard.java), [`StudyingNow`](../social-service/src/main/java/com/studyanchor/social/domain/StudyingNow.java).

## Q3 — Distraction spike detection (FR-12)

```sql
CREATE STREAM distraction_spikes AS
  SELECT userId,
         COUNT(*)    AS distractionCount,
         WINDOWSTART AS windowStart,
         WINDOWEND   AS windowEnd
  FROM focus_distractions
       WITH (TIMESTAMP = loggedAt)
  WINDOW TUMBLING (SIZE 30 MINUTES, GRACE PERIOD 1 MINUTE)
  GROUP BY userId
  EMIT CHANGES
  HAVING COUNT(*) = 3;          -- fires once per window, on the 3rd distraction
```

- **Input:** `focus.distractions`. **Output:** `focus.alerts` (`DistractionSpikeDetected`). **State store:** `distraction-spike-store`.
- **Windows** are aligned to the clock (10:00–10:30, 10:30–11:00, …) and counted per student across all their sessions (FR-12 AC8).
- **Caching is turned off** on the count, so every new count reaches the filter. Otherwise Kafka Streams could merge counts 2→3→4 and the "= 3" check would never see 3. Filtering on exactly 3 means a 4th distraction in the same window publishes nothing.
- **Downstream:** Planner consumes `focus.alerts` and creates one reminder per student and window. A repeated alert is ignored through the unique `triggerKey` `spike:<windowStart>`.
- **Code:** [`DistractionSpikeTopology`](../focus-service/src/main/java/com/studyanchor/focus/infrastructure/DistractionSpikeTopology.java), [`DistractionSpikeDetected`](../focus-service/src/main/java/com/studyanchor/focus/domain/DistractionSpikeDetected.java), [`FocusAlertsConsumer`](../planner-service/src/main/java/com/studyanchor/planner/infrastructure/FocusAlertsConsumer.java).
