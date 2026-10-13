# 05 — Event Catalogue

> Status: draft v0.1 (2026-10-13). Drafted with AI assistance; reviewed by the author.

## Conventions
- Format: JSON.
- **Message key: `userId`** for every event. All events for one user land in the same partition, so they stay in order (a session's "ended" can never be processed before its "started"). Kafka Streams aggregations per user also rely on this.
- Every event has these envelope fields:

| Field | Type | Notes |
|---|---|---|
| `eventId` | UUID | Unique per event (consumers can ignore duplicates) |
| `eventType` | String | e.g. `FocusSessionEnded` |
| `occurredAt` | ISO-8601 instant | When it happened in the domain |
| `userId` | String | Also the message key |

- Each service defines **its own Java record** for each event it publishes or consumes. There is no shared events library: this document is the contract (DDD "published language"), and services stay independently deployable.

## Topics

| Topic | Events | Producer | Consumers |
|---|---|---|---|
| `planner.topics` | TopicCreated, TopicDeleted *(TopicDeleted added 2026-10-14)* | Planner | Tutor |
| `tutor.materials` | MaterialAdded | Tutor | Planner |
| `tutor.understanding` | UnderstandingRecorded | Tutor | Planner |
| `focus.sessions` | FocusSessionStarted, FocusSessionEnded | Focus | Social (streams, FR-09), Focus (streams, FR-10), Planner |
| `focus.distractions` | DistractionLogged | Focus | Focus (streams, FR-12) |
| `focus.alerts` | DistractionSpikeDetected | Focus (streams) | Planner |

`FocusSessionStarted` and `FocusSessionEnded` share one topic so their order per user is guaranteed.

## Payloads (in addition to the envelope)

### TopicCreated — FR-01
| Field | Type |
|---|---|
| topicId | UUID |
| courseId | UUID |
| courseCode | String |
| topicName | String |

### TopicDeleted — FR-01 *(decided 2026-10-14)*
Published when a topic is deleted, or once per topic when its course is deleted. Tutor removes its copy of the topic.

| Field | Type |
|---|---|
| topicId | UUID |
| courseId | UUID |

### MaterialAdded — FR-02
| Field | Type |
|---|---|
| materialId | UUID |
| topicId | UUID |
| title | String |

### UnderstandingRecorded — FR-07
| Field | Type |
|---|---|
| tutoringSessionId | UUID |
| topicId | UUID |
| level | int (1–5) |

### FocusSessionStarted — FR-05
| Field | Type |
|---|---|
| sessionId | UUID |
| timeBlockId | UUID, nullable |
| topicId | UUID, nullable |
| plannedMinutes | int |
| startedAt | instant |

### FocusSessionEnded — FR-05
| Field | Type |
|---|---|
| sessionId | UUID |
| timeBlockId | UUID, nullable |
| topicId | UUID, nullable |
| actualSeconds | long — exact duration in whole seconds *(decided 2026-10-13, replaces `actualMinutes`)* |
| startedAt | instant — FR-10 counts the session toward its start day *(decided 2026-10-13)* |
| endedAt | instant |

### DistractionLogged — FR-06
| Field | Type |
|---|---|
| sessionId | UUID |
| distractionType | PHONE \| SOCIAL_MEDIA \| OTHER_SITE \| THOUGHT \| OTHER |
| site | String, nullable |
| offPolicy | boolean |

### DistractionSpikeDetected — FR-12
Published once per student per fixed 30-minute window, when the window reaches 3 distractions; `distractionCount` is therefore 3. `occurredAt` is when the stream detected the spike (moments after the 3rd distraction). Focus sets each Kafka record's timestamp to the event's `occurredAt`, so the window uses the time the distraction was logged. Planner ignores a repeat with the same `windowStart`. *(decided 2026-10-14)*

| Field | Type |
|---|---|
| distractionCount | int |
| windowStart | instant |
| windowEnd | instant |

## Example

```json
{
  "eventId": "3f1c2a9e-8b7d-4c1e-9f0a-1b2c3d4e5f60",
  "eventType": "FocusSessionEnded",
  "occurredAt": "2026-10-14T03:40:00Z",
  "userId": "alice",
  "sessionId": "a7d0c1b2-1111-4222-8333-944455566677",
  "timeBlockId": null,
  "topicId": "c0ffee00-0000-4000-8000-000000000001",
  "actualSeconds": 2715,
  "startedAt": "2026-10-14T02:54:45Z",
  "endedAt": "2026-10-14T03:40:00Z"
}
```
