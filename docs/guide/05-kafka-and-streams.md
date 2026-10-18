# 5. Kafka events and real-time streams

This part shows how events travel between services (publish → topic → consume) and how the three **stream processors** turn those events into live answers: progress and streak (FR-10), "studying now" (FR-09), and distraction spikes (FR-12).

> Origin: written by Claude (AI) from the project's code, reviewed by the author. Code snippets are copied from the repo; `…` marks lines left out. The SQL-like queries are also in [06-stream-queries.md](../06-stream-queries.md).

---

## 5.1 Kafka in two minutes

| Word | Meaning | In StudyAnchor |
|---|---|---|
| **Broker** | The Kafka server that stores messages | One broker in Docker, `localhost:9092` |
| **Topic** | A named, append-only log of messages (a noticeboard) | 6 topics, e.g. `focus.sessions` |
| **Partition** | A topic is split into partitions; order is guaranteed *within* a partition | 3 partitions per topic |
| **Key** | Decides the partition: same key → same partition → kept in order | Always the `userId` |
| **Producer** | Writes messages | `StreamBridgeEventPublisher` in Planner, Tutor, Focus |
| **Consumer group** | Readers that share the work; each group gets every message once | `planner-service`, `tutor-service`, … |
| **Offset** | A reader's bookmark in a partition | Kafka remembers it per group |

**Why the key matters:** with `userId` as the key, a student's `FocusSessionStarted` and `FocusSessionEnded` always arrive in the right order, and stream processors can group by student without reshuffling data.

---

## 5.2 Publishing: one pattern everywhere

Every publisher follows the steps from [part 03](03-inside-a-service.md) (steps 4–7): **aggregate registers event → service saves → service publishes through the `EventPublisher` port → `StreamBridgeEventPublisher` sends to Kafka.**

The publisher (focus-service; Planner and Tutor have their own copies):

```java
var message = MessageBuilder.withPayload(event)
        .setHeader(KafkaHeaders.KEY, event.userId().getBytes(StandardCharsets.UTF_8))
        // FR-12: record time = domain time, so the spike windows count distractions by when they were logged
        .setHeader(KafkaHeaders.TIMESTAMP, event.occurredAt().toEpochMilli())
        .build();
if (!streamBridge.send(binding, message)) { … }
```

`application.yml` maps each *binding* name to a real topic:

```yaml
focusSessions-out-0:
  destination: focus.sessions
focusDistractions-out-0:
  destination: focus.distractions
```

| Service | Publishes | To topic |
|---|---|---|
| Planner | `TopicCreated`, `TopicDeleted` | `planner.topics` |
| Tutor | `MaterialAdded` | `tutor.materials` |
| Tutor | `UnderstandingRecorded` | `tutor.understanding` |
| Focus | `FocusSessionStarted`, `FocusSessionEnded` | `focus.sessions` |
| Focus | `DistractionLogged` | `focus.distractions` |
| Focus (spike stream) | `DistractionSpikeDetected` | `focus.alerts` |

---

## 5.3 Consuming: a function per topic

With Spring Cloud Stream, a consumer is just a Spring bean of type `Consumer<SomeEvent>`. Spring calls it once per message. Example: `planner-service/…/infrastructure/FocusAlertsConsumer.java`:

```java
@Bean
Consumer<DistractionSpikeEvent> focusAlerts(ReminderService reminders) {
    return event -> {
        log.info("Received {} for user {}: {} distractions in {} – {}", event.eventType(), event.userId(),
                event.distractionCount(), event.windowStart(), event.windowEnd());
        reminders.onDistractionSpike(event.userId(), event.distractionCount(), event.windowStart());
    };
}
```

And the matching config in `planner-service/src/main/resources/application.yml`:

```yaml
spring.cloud.function.definition: tutorMaterials;tutorUnderstanding;focusAlerts;focusSessions
…
focusAlerts-in-0:
  destination: focus.alerts
  group: planner-service
```

- The bean name `focusAlerts` + `-in-0` = the binding name, so Spring connects the function to the topic `focus.alerts`.
- `group: planner-service` makes it a consumer group, so Kafka remembers where Planner stopped reading. After a restart it carries on instead of re-reading everything.
- The consumer only **translates** (JSON → method call) and hands over to the service layer, just like a controller does for HTTP.
- `DistractionSpikeEvent` is **Planner's own copy** of the event's fields. Services share no code, only the JSON contract in [05-events.md](../05-events.md).

**All plain consumers:**

| Consumer (file) | Service | Topic | What it does |
|---|---|---|---|
| `PlannerTopicsConsumer` | Tutor | `planner.topics` | Adds or removes its `TopicReference` copy |
| `TutorMaterialsConsumer` | Planner | `tutor.materials` | Counts materials per topic |
| `TutorUnderstandingConsumer` | Planner | `tutor.understanding` | Stores the topic's latest understanding level |
| `FocusSessionsConsumer` | Planner | `focus.sessions` | Marks time blocks `STARTED` / `COMPLETED` |
| `FocusAlertsConsumer` | Planner | `focus.alerts` | Creates a reminder quoting a goal or value |

**A special case: Tutor's read model.** Tutor keeps its topic copy in an in-memory database, which is empty after a restart. So Tutor re-reads `planner.topics` from the very first message on every start:

```yaml
plannerTopics-in-0:
  consumer:
    # The topic copy lives in in-memory H2, so rebuild it from the first event on every start
    resetOffsets: true
    startOffset: earliest
```

That works because Kafka *keeps* old messages: the topic itself is the history.

---

## 5.4 Stream processing: the ideas you need

A plain consumer looks at one message at a time and forgets it. A **stream processor** (Kafka Streams) keeps **state** across messages, so it can count, sum and remember, and it updates the answer the moment a new event arrives.

| Idea | Meaning | Used in |
|---|---|---|
| **KStream** | An endless flow of events ("this happened, then this…") | input of all three |
| **KTable** | The *latest value per key* ("what is true now") | FR-09 presence |
| **Tumbling window** | Fixed, non-overlapping time buckets (each day; each 30 minutes) | FR-10 (1 day), FR-12 (30 min) |
| **Event time** | Count an event by *when it happened*, not when it arrived | all three |
| **Grace period** | How long a window stays open for late events | FR-10: 1 day, FR-12: 1 minute |
| **State store** | Kafka Streams' local key-value store (RocksDB), backed up to a Kafka *changelog* topic | all three |
| **Interactive query** | A REST endpoint reads the state store directly, with no database | FR-09, FR-10 |
| **Tombstone** | A `null` value that deletes a key from a table | FR-09 |

In the code, a topology is a Spring bean of type `Consumer<KStream<…>>` (reads only) or `Function<KStream<…>, KStream<…>>` (reads and writes a new topic). The Kafka Streams binder runs it.

---

## 5.5 FR-10: focus progress and streak (Focus)

**Question it answers in real time:** *"How many seconds has this student focused on each day, and what is their streak?"*

**As a query** (SQL-like description; ksqlDB is not used):

```sql
CREATE TABLE focus_progress AS
  SELECT userId, WINDOWSTART AS sydneyDay, SUM(actualSeconds) AS seconds
  FROM focus_sessions WITH (TIMESTAMP = startedAt shifted to the Sydney day)
  WHERE eventType = 'FocusSessionEnded'
  WINDOW TUMBLING (SIZE 1 DAY, GRACE PERIOD 1 DAY, RETENTION 3650 DAYS)
  GROUP BY userId;
```

**As Java:** `focus-service/…/infrastructure/FocusProgressTopology.java`

```java
@Bean
Consumer<KStream<String, FocusSessionMessage>> focusProgress() {
    return sessions -> sessions
            .filter((userId, message) -> message != null && message.isEnded())        // WHERE ended
            .mapValues(FocusSessionMessage::actualSeconds)                              // keep seconds only
            .groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))                   // GROUP BY userId
            .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofDays(1), GRACE))          // per day, 1 day grace
            .reduce(Long::sum,                                                          // SUM(actualSeconds)
                    Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(STORE)    // into focus-progress-store
                            .withKeySerde(Serdes.String())
                            .withValueSerde(Serdes.Long())
                            .withRetention(RETENTION)                                   // keep 10 years
                            .withLoggingEnabled(CHANGELOG_CONFIG));
}
```

Read the comments on the right as a line-by-line match with the SQL.

**The Sydney-midnight trick.** Kafka's 1-day windows start at *UTC* midnight, but a student's day starts at *Sydney* midnight. `SydneyDayTimestampExtractor` re-reads the session's `startedAt` as if Sydney local time were UTC (00:30 Sydney → 00:30 UTC), so each window is exactly one Sydney day, even on 23- or 25-hour daylight-saving days. It uses `startedAt`, so a session from 23:50 to 00:20 counts for the day it *started* (a decision recorded in the ai-log).

**Reading the answer (interactive query):** `KafkaStreamsFocusHistory.java`

```java
ReadOnlyWindowStore<String, Long> store =
        queryService.getQueryableStore(FocusProgressTopology.STORE, QueryableStoreTypes.windowStore());
try (var windows = store.fetch(userId, Instant.EPOCH, to)) {
    windows.forEachRemaining(kv -> result.put(SydneyDayTimestampExtractor.sydneyDayOf(kv.key), kv.value));
}
```

Then the domain value object `FocusProgress` computes *today*, *last 7 days* and the **streak** (consecutive days with at least 25 focus minutes). That is pure Java with unit tests. `GET /progress/{userId}` returns it:

```json
{"userId":"demo-student","today":{"date":"2026-10-19","focus":"0m 4s","seconds":4},
 "last7Days":[…,{"date":"2026-10-19","focus":"0m 4s","seconds":4}],"streakDays":0}
```

If the store is still starting up, the endpoint returns **503** ("try again shortly") instead of wrong numbers.

---

## 5.6 FR-09: who is studying now (Social)

**Question:** *"Which of my friends are in a focus session right now, and how many people in this room?"*

**As a query:**

```sql
CREATE TABLE presence AS
  SELECT userId, LATEST_BY_OFFSET(sessionId), LATEST_BY_OFFSET(topicId), LATEST_BY_OFFSET(startedAt)
  FROM focus_sessions
  GROUP BY userId;
  -- FocusSessionStarted upserts the row; FocusSessionEnded deletes it (tombstone)
```

**As Java:** `social-service/…/infrastructure/PresenceTopology.java`

```java
@Bean
Consumer<KStream<String, FocusSessionEvent>> presence() {
    return events -> events
            .filter((userId, event) -> event != null && (event.isStarted() || event.isEnded()))
            .peek((userId, event) -> log.info("Presence: {} for user {} (session {}, topic {})", …))
            // A null value is a tombstone: toTable deletes the key, so the user is no longer "studying now"
            .mapValues(event -> event.isStarted() ? event.toPresence() : null)
            .toTable(Materialized.<String, Presence, KeyValueStore<Bytes, byte[]>>as(STORE) …);
}
```

- **Started** → a `Presence` row for that student. **Ended** → `null` → the row is deleted. The table therefore always holds *exactly* the students studying right now.
- `KafkaStreamsPresenceBoard` reads it with `store.get(userId)` for each friend.
- **Privacy is applied when someone asks**, not in the stream: the domain service `StudyingNow` only shows friends whose `SharingSetting` is on. Turning sharing off hides you immediately, with no event needed.
- Social **never calls Focus**. Presence is rebuilt purely from events, so Social works even if Focus is down.

---

## 5.7 FR-12: distraction spike detection (Focus → Planner)

**Question:** *"Has this student been distracted 3 times within one 30-minute window?"* If so, remind them why they study.

**As a query:**

```sql
CREATE STREAM distraction_spikes AS
  SELECT userId, COUNT(*) AS distractionCount, WINDOWSTART, WINDOWEND
  FROM focus_distractions WITH (TIMESTAMP = loggedAt)
  WINDOW TUMBLING (SIZE 30 MINUTES, GRACE PERIOD 1 MINUTE)
  GROUP BY userId
  EMIT CHANGES
  HAVING COUNT(*) = 3;
```

**As Java:** `focus-service/…/infrastructure/DistractionSpikeTopology.java`. This one is a `Function`: it reads one topic and **writes** another.

```java
@Bean
Function<KStream<String, DistractionMessage>, KStream<String, DistractionSpikeDetected>> distractionSpikes(
        Clock clock) {
    return distractions -> distractions
            .filter((userId, message) -> message != null && message.isLogged())
            .groupByKey(Grouped.keySerde(Serdes.String()))
            .windowedBy(TimeWindows.ofSizeAndGrace(DistractionSpikeDetected.WINDOW, GRACE))   // 30 min
            // Caching off: every new count reaches the filter, so "count = 3" is never merged away
            .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(STORE).withCachingDisabled())
            .toStream()
            .filter((window, count) -> DistractionSpikeDetected.firesAt(count))              // HAVING = 3
            .map((window, count) -> KeyValue.pair(window.key(), DistractionSpikeDetected.of(window.key(),
                    window.window().startTime(), window.window().endTime(), clock.instant())))
            …;
}
```

The rule itself ("exactly 3, in 30 minutes") lives in the **domain** event class, so it is unit-tested without Kafka:

```java
public static final Duration WINDOW = Duration.ofMinutes(30);
public static final int THRESHOLD = 3;

/** True only when a window's count reaches exactly the threshold, so each window alerts once. */
public static boolean firesAt(long countInWindow) {
    return countInWindow == THRESHOLD;
}
```

**The full chain:**

```
POST /sessions/{id}/distractions  ×3
  → DistractionLogged ×3 on focus.distractions
  → spike stream counts 1, 2, 3 → fires on 3
  → DistractionSpikeDetected on focus.alerts
  → Planner FocusAlertsConsumer → ReminderService
  → Reminder quoting the student's own goal or value  (GET /reminders)
```

- **Why "= 3" and not "≥ 3":** a 4th distraction in the same window must not send a second reminder.
- **Why caching is off:** Kafka Streams normally batches updates, so counts 2 → 3 → 4 could arrive at the filter as just "4", and the "= 3" check would never fire.
- **Duplicate safety:** Planner stores each reminder with a unique `triggerKey` `spike:<windowStart>`, so even if Kafka delivered the alert twice, there is only one reminder.

---

## 5.8 Two real problems found by live testing

Both are recorded in [progress.md](../progress.md). They are good material for report §3.7 and §5.2:

1. **Kafka 4 rejected "future" records.** Because of the Sydney trick (5.5), the progress store writes records stamped up to 11 hours ahead of UTC. Kafka 4 brokers refuse records more than 1 hour in the future by default, so the progress stream died about 30 seconds after a session ended. Unit tests could not catch this; a live test did. The fix: create that store's changelog topic allowing up to 1 day ahead (`CHANGELOG_CONFIG` in `FocusProgressTopology`).
2. **Merged counts.** Without `withCachingDisabled()`, the spike filter could miss the exact count of 3 (5.7).

---

## 5.9 Try it yourself

With everything running ([01-run-it.md](01-run-it.md)), using Postman:

| Do this | Watch this |
|---|---|
| Folder **5 FR-05**: Start session | Social window: `Presence: FocusSessionStarted …` |
| Folder **7 FR-09 Studying now** | The friend's board lists the student while the session is open, and drops them after it ends |
| Folder **8 FR-06 + FR-12**: log 3 distractions | Focus window: `Detected DistractionSpikeDetected …`; Planner window: `Received DistractionSpikeDetected …`; then the reminder request shows the quote |
| Folder **9 FR-10**, or open `http://localhost:8083/progress/<alice-name>` | Seconds added to today |

---

**Next:** [06: AI agents](06-ai-agents.md) shows how the two LangChain4j agents call tools, and how the Java code keeps them within the rules.
