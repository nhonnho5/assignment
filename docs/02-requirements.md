# 02 — Functional Requirements

> Status: draft v0.1 (2026-10-13). Requirements chosen by the author; acceptance criteria drafted with AI assistance and reviewed by the author.
> Every requirement has an ID. Code, commits and tests reference these IDs.

## Traceability summary

| ID | Requirement | Service | Objective | Type |
|---|---|---|---|---|
| FR-01 | Manage courses, topics and deadlines | Planner | O1 | |
| FR-02 | Add study material to a topic | Tutor | O2 | |
| FR-03 | Set weekly study availability | Planner | O1 | |
| FR-04 | AI-generated weekly study plan | Planner | O1 | **Agentic AI** |
| FR-05 | Start and end a focus session | Focus | O3 | |
| FR-06 | Log a distraction | Focus | O3 | |
| FR-07 | Socratic AI tutor | Tutor | O2 | **Agentic AI** |
| FR-08 | Friends, study rooms and sharing settings | Social | O4 | |
| FR-09 | Live "studying now" board | Social | O4 | **Stream processing** |
| FR-10 | Real-time focus progress and streak | Focus | O3 | **Stream processing** |
| FR-11 | Personal goals and value statements | Planner | O5 | |
| FR-12 | Contextual goal reminders | Planner (+ Focus streams) | O5, O3 | **Stream processing** |

---

## FR-01 — Manage courses, topics and deadlines
**Story:** As a student, I want to register my courses, their topics and assessment deadlines, so the system knows what I need to study and by when.
**Relevance:** O1 — planning is impossible without knowing the workload.
**Acceptance criteria**
1. A student can create a course (code, name) and add topics to it.
2. A student can create a deadline with title, due date-time, weight (1–100 %) and one or more topic IDs from the same course.
3. A deadline with a due date in the past is rejected.
4. Creating a topic publishes `TopicCreated`.

## FR-02 — Add study material to a topic
**Story:** As a student, I want to add my lecture notes or readings to a topic, so the AI tutor can ground its help in my actual course content.
**Relevance:** O2 — tutoring is more accurate and relevant when based on course material.
**Acceptance criteria**
1. A student can add material (title, text content up to 50,000 characters) to a topic.
2. Material for a topic unknown to the Tutor service is rejected (Tutor knows topics via `TopicCreated`).
3. Adding material publishes `MaterialAdded`.

## FR-03 — Set weekly study availability
**Story:** As a student, I want to tell the system when I'm available to study each week, so plans fit my real life.
**Relevance:** O1 — unrealistic plans are abandoned; plans must respect actual free time.
**Acceptance criteria**
1. A student can define availability slots (day of week, start time, end time).
2. Overlapping slots on the same day are rejected.
3. A slot must be at least 25 minutes long.

## FR-04 — AI-generated weekly study plan (Agentic AI)
**Story:** As a student, I want the system to build my week's study time blocks for me, so I don't get stuck deciding what to study.
**Relevance:** O1 — removes the planning step that ADHD makes hardest.
**Acceptance criteria**
1. A student can request a plan for a given week start date, which must be a Monday (otherwise 400). *(decided 2026-10-13)*
2. The planning agent uses tools to read: deadlines, availability, topic understanding levels, and recent focus statistics (via a REST call to the Focus service).
3. Time blocks are created only inside availability slots, are 25–90 minutes long, and have at least 10 minutes between consecutive blocks.
4. Topics with nearer, heavier deadlines and lower understanding levels are prioritised.
5. Each block includes a one-sentence rationale.
6. If available time is insufficient, the plan says so explicitly instead of overbooking.
7. All block constraints (3) are enforced in domain code, not only in the prompt; invalid blocks proposed by the agent are rejected and reported back to it.

## FR-05 — Start and end a focus session
**Story:** As a student, I want to start and stop a timed focus session, so my study time is tracked.
**Relevance:** O3 — tracking is the basis of feedback and habit building.
**Acceptance criteria**
1. A student can start a session with optional time block ID, optional topic ID, planned minutes (10–180) and an allowed-sites list.
2. A student can have only one active session at a time (second start returns 409 Conflict).
3. Ending a session records actual minutes.
4. Starting publishes `FocusSessionStarted`; ending publishes `FocusSessionEnded`.

## FR-06 — Log a distraction
**Story:** As a student, I want to quickly log when I get distracted, so I can see my patterns and get support.
**Relevance:** O3 — awareness of distraction is the first step to reducing it.
**Acceptance criteria**
1. A distraction can only be logged during an active session.
2. It has a type (PHONE, SOCIAL_MEDIA, OTHER_SITE, THOUGHT, OTHER), optional site and optional note.
3. If a site is given and is not in the session's allowed-sites list, the distraction is marked off-policy.
4. Logging publishes `DistractionLogged`.

## FR-07 — Socratic AI tutor (Agentic AI)
**Story:** As a student, I want an AI tutor that guides me with questions and hints instead of giving me the answer, so I actually learn.
**Relevance:** O2 — counters passive "skip to the answer" AI use.
**Acceptance criteria**
1. A student starts a tutoring session with a topic ID and a question, then continues with messages.
2. The tutor's first response is a guiding question or hint, never a full solution.
3. The full solution can only be produced through a `revealSolution` tool, which code-enforces that at least 2 student attempts have been recorded in this session; otherwise the tool refuses and the tutor gives another hint.
4. The tutor grounds its help in the topic's material when material exists.
5. At the end, the tutor asks a check-understanding question and records an understanding level (1–5), publishing `UnderstandingRecorded`.

## FR-08 — Friends, study rooms and sharing settings
**Story:** As a student, I want to connect with friends and join study rooms, so I don't study alone.
**Relevance:** O4 — social accountability.
**Acceptance criteria**
1. A student can send a friend request; the other user can accept it.
2. A student can create a study room and others can join it by room ID.
3. Each student has a sharing setting, `false` by default; presence is visible to others only when `true`.

## FR-09 — Live "studying now" board (Stream processing)
**Story:** As a student, I want to see which friends are studying right now, so I feel accompanied and motivated to start.
**Relevance:** O4 — the "study together" effect.
**Acceptance criteria**
1. A student can list friends currently in a focus session, with topic (if any) and minutes elapsed.
2. A student can see how many members of a study room are currently studying.
3. The board reflects session start/end within a few seconds.
4. Implemented as a Kafka Streams stateful aggregation over `focus.sessions`, queried via interactive queries.
5. Users with sharing off never appear. Presence is stored for every user; the sharing filter is applied at query time against Social's `SharingSetting`, so turning sharing off takes effect immediately. *(decided 2026-10-13)*

## FR-10 — Real-time focus progress and streak (Stream processing)
**Story:** As a student, I want to see my focus minutes today, this week and my streak, so I can see that I'm improving.
**Relevance:** O3 — visible progress reinforces habit.
**Acceptance criteria**
1. A student can view focus minutes for today and for each of the last 7 days.
2. A student can view their current streak (consecutive days with ≥ 25 focus minutes). A "day" is a calendar day in `Australia/Sydney`. *(decided 2026-10-13)*
3. Values update within a few seconds of a session ending.
4. Implemented as a Kafka Streams windowed aggregation (1-day windows aligned to `Australia/Sydney` midnight) over `FocusSessionEnded` events.

## FR-11 — Personal goals and value statements
**Story:** As a student, I want to write down my goals and the ideas that matter to me, so the system can remind me why I'm studying.
**Relevance:** O5 — intrinsic motivation.
**Acceptance criteria**
1. A student can create goals (title, "why" statement, optional target date) and mark them achieved.
2. A student can create value statements (short text, max 280 characters).

## FR-12 — Contextual goal reminders (Stream processing trigger)
**Story:** As a student, I want reminders of my own goals and values when I'm struggling, or at random times, so I stay connected to my purpose.
**Relevance:** O5 and O3 — timely motivation when focus breaks down.
**Acceptance criteria**
1. When a student logs ≥ 3 distractions within a 30-minute window, the Focus service's stream processor publishes `DistractionSpikeDetected` once for that window.
2. On `DistractionSpikeDetected`, the Planner creates a reminder that quotes one of the student's goals or value statements.
3. When the Planner consumes a `FocusSessionEnded` with a `timeBlockId`, that time block is marked completed. *(decided 2026-10-13)*
4. When a scheduled time block ends and is not completed, the Planner creates a "missed block" reminder, once per block. Detected by a Spring `@Scheduled` job that runs every minute. *(decided 2026-10-13)*
5. Optionally, random reminders are created at a configurable frequency (max N per day).
6. A student can list unread reminders and mark them read.
7. *(Stretch)* the reminder text is phrased by the LLM in an encouraging tone, using the student's own words.

---

## Non-functional notes
- Each service owns its own H2 database; no shared tables.
- Services communicate asynchronously via Kafka, plus a small number of REST calls (documented in `03-bounded-contexts.md`).
- No secrets in the repository; the LLM API key is read from an environment variable.
