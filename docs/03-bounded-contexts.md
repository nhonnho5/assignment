# 03 — Bounded Contexts

> Status: draft v0.1 (2026-10-13). Service split decided by the author; details drafted with AI assistance and reviewed by the author.

## Overview

| Service | Port | Responsibility | FRs |
|---|---|---|---|
| planner-service | 8081 | What to study and when; why the student studies | FR-01, FR-03, FR-04, FR-11, FR-12 |
| tutor-service | 8082 | Learning content and guided AI tutoring | FR-02, FR-07 |
| focus-service | 8083 | Focus sessions, distractions, focus analytics | FR-05, FR-06, FR-10, FR-12 (detection) |
| social-service | 8084 | Friends, study rooms, live presence | FR-08, FR-09 |

## Planner context
- **Owns:** Course, Topic, Deadline, Availability, StudyPlan / TimeBlock, Goal, ValueStatement, Reminder.
- **Publishes:** `TopicCreated`.
- **Consumes:** `MaterialAdded` (to know which topics have material), `UnderstandingRecorded` (to prioritise topics), `FocusSessionEnded` (to mark time blocks completed), `DistractionSpikeDetected` (to create reminders).
- **Calls (REST):** focus-service `GET /progress/{userId}` — used by the planning agent's `getFocusStats` tool.
- **Agent:** Planning agent (FR-04).
- **Note:** Goals and reminders are kept here rather than in a separate service: reminders are about *why and what* the student studies, and they need goals, values and time blocks together. A separate Motivation service is a possible future split.

## Tutor context
- **Owns:** Material, TutoringSession (with Attempts and messages), local TopicReference (read model).
- **Publishes:** `MaterialAdded`, `UnderstandingRecorded`.
- **Consumes:** `TopicCreated` (to build its local topic read model).
- **Agent:** Socratic tutor agent (FR-07).
- **Note:** Tutor keeps its own copy of topic names instead of calling Planner, so tutoring keeps working if Planner is down.

## Focus context
- **Owns:** FocusSession (with Distractions).
- **Publishes:** `FocusSessionStarted`, `FocusSessionEnded`, `DistractionLogged`, `DistractionSpikeDetected` (from the stream processor).
- **Consumes (Kafka Streams):** its own `focus.sessions` and `focus.distractions` topics for FR-10 and FR-12.
- **Exposes (REST):** `GET /progress/{userId}` — backed by the stream processor's state store.

## Social context
- **Owns:** Friendship, StudyRoom (with members), SharingSetting.
- **Consumes (Kafka Streams):** `focus.sessions` → presence state store (FR-09).
- **Note:** Presence is derived state, rebuilt from events — Social never asks Focus directly who is studying.

## Context map

```
           TopicCreated
 Planner ───────────────────▶ Tutor
    ▲  ◀─────────────────────  │
    │   MaterialAdded,         │
    │   UnderstandingRecorded  │
    │
    │ FocusSessionEnded, DistractionSpikeDetected
    │ (+ REST: GET /progress/{userId})
    │
  Focus ──────────────────────▶ Social
          FocusSessionStarted / FocusSessionEnded
```

- **Planner → Tutor:** Planner is upstream for topics; Tutor conforms to Planner's topic identity (read model built from events).
- **Focus → Social, Planner:** Focus is upstream; it publishes facts about study sessions that others react to.
- **Tutor → Planner:** Tutor publishes learning outcomes that Planner uses for prioritisation.

## Data ownership

| Data | Owner | Copies elsewhere |
|---|---|---|
| Course, Topic, Deadline | Planner | Tutor keeps TopicReference (id, name, course code) |
| Availability, TimeBlock, StudyPlan | Planner | — |
| Goal, ValueStatement, Reminder | Planner | — |
| Material, TutoringSession, Attempt | Tutor | Planner keeps per-topic material count and latest understanding level |
| FocusSession, Distraction | Focus | Social keeps presence state (Kafka Streams store) |
| Focus progress (minutes, streak) | Focus (Kafka Streams store) | — |
| Friendship, StudyRoom, SharingSetting | Social | — |
