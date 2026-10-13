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
5. A Course is one aggregate that holds its Topics and Deadlines (URLs `/courses/{id}/topics`, `/courses/{id}/deadlines`). Courses belong to the student in `X-User-Id`; another student's course → 404. *(decided 2026-10-14)*
6. Text limits (trimmed): course code 1–20 characters, upper-cased (`csci318` = `CSCI318`); course name, topic name and deadline title 1–100. The same course code twice for one student → 409. The same topic name twice in a course (case-insensitive) → 409. *(decided 2026-10-14)*
7. A deadline needs at least one topic ID; a topic ID that is not in that course → 400. Weight is a whole number 1–100; a fractional weight is not rejected but stored without its fraction (`12.5` → `12`) *(decided 2026-10-14)*. The total weight of a course's deadlines may not exceed 100 % → 409. *(decided 2026-10-14)*
8. The due date-time is sent as Australia/Sydney local time (`"dueAt": "2026-10-30T23:59"`), stored as UTC and shown back in Sydney time. A due date-time at or before the current instant is "in the past" → 400. *(decided 2026-10-14)*
9. Courses, topics and deadlines can be listed, read and deleted; there is no edit. Deleting a topic publishes `TopicDeleted`. Deleting a topic that a deadline still uses → 409 (delete the deadline first). Deleting a course deletes its topics and deadlines and publishes `TopicDeleted` for each topic. *(decided 2026-10-14)*
10. Tutor keeps a copy of each topic (id, name, course code, owner) built from `TopicCreated` / `TopicDeleted`; `GET /topics` on the Tutor lists the student's known topics. *(decided 2026-10-14)*

## FR-02 — Add study material to a topic
**Story:** As a student, I want to add my lecture notes or readings to a topic, so the AI tutor can ground its help in my actual course content.
**Relevance:** O2 — tutoring is more accurate and relevant when based on course material.
**Acceptance criteria**
1. A student can add material (title, text content up to 50,000 characters) to a topic.
2. Material for a topic unknown to the Tutor service is rejected (Tutor knows topics via `TopicCreated`).
3. Adding material publishes `MaterialAdded`.
4. Material is added by uploading a file: `POST /topics/{topicId}/materials` as multipart form data with `file` (a `.txt` or `.md` file in UTF-8, or a `.pdf`) and an optional `title` (default: the file name without its extension). Any other file type → 400. The text of a PDF is extracted on upload; a PDF that cannot be read (broken, password-protected) or has no text (e.g. scanned pages) → 400. The 50,000-character limit applies to the extracted text. *(decided 2026-10-14; PDF approved by the coordinator 2026-10-14)*
5. Title 1–100 characters and content 1–50,000 characters, both trimmed; blank or too long → 400. A topic unknown to Tutor or owned by another student (`X-User-Id`) → 404. *(decided 2026-10-14)*
6. `GET /topics/{topicId}/materials` lists a topic's materials (id, title, characters, added at; no content); `GET /materials/{id}` returns one material as JSON with its full text; another student's material → 404. There is no edit or delete. *(decided 2026-10-14)*
7. When Tutor receives `TopicDeleted`, it also removes that topic's materials (no event is published). *(decided 2026-10-14)*
8. Planner keeps the IDs of the materials added to each of its topics, from `MaterialAdded`, and `GET /courses/{id}` shows `materialCount` per topic. A repeated `MaterialAdded` is counted once; one for an unknown topic is ignored. *(decided 2026-10-14)*

## FR-03 — Set weekly study availability
**Story:** As a student, I want to tell the system when I'm available to study each week, so plans fit my real life.
**Relevance:** O1 — unrealistic plans are abandoned; plans must respect actual free time.
**Acceptance criteria**
1. A student can define availability slots (day of week, start time, end time).
2. Overlapping slots on the same day are rejected.
3. A slot must be at least 25 minutes long.
4. Slots are added one at a time (`POST /availability/slots`), removed by ID (`DELETE /availability/slots/{id}`) and listed with `GET /availability`. Overlap with an existing slot → 409; invalid times or a slot shorter than 25 minutes → 400. Removing an unknown slot → 404. *(decided 2026-10-14)*
5. Times are `"HH:mm"` Australia/Sydney wall-clock times in whole minutes. The end must be after the start on the same day (no slot crosses midnight). Exactly 25 minutes is allowed. Touching slots (09:00–10:00 and 10:00–11:00) do not overlap. *(decided 2026-10-14)*

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
8. Endpoints: `POST /plans` with `{weekStart, replace}` → 201 with the plan; `GET /plans/{weekStart}` → the plan, or 404. One plan per student per week. If the week already has a plan and `replace` is not `true` → 409 with a message that reminds the student of the existing plan and tells them to resend with `"replace": true` to change it; with `"replace": true` the new plan replaces the old one (reminders already created stay). *(decided 2026-10-14)*
9. A week that has already ended (its Sunday is before today in Sydney) → 400. No availability slots, or no topics → 400, checked before the LLM is called. *(decided 2026-10-14)*
10. Block rules in detail (AC3): a block has a Sydney date and `HH:mm` start/end in whole minutes; the date is in the plan's week; the block fits inside **one** availability slot of that weekday (touching slots are not joined); 25–90 minutes; at least 10 minutes from any other block that day; starts after the current instant (planning the current week uses only the time left, and no block is "missed" at birth); the topic is one of the student's topics; the rationale is 1–200 characters. *(decided 2026-10-14)*
11. Tools: `getDeadlines`, `getAvailability` (the week's slots on real dates), `getTopics` (with understanding level and priority), `getFocusStats` (Focus `GET /progress/{userId}`; if Focus cannot be reached the tool says so and planning continues), `addBlocks(blocks)` (valid blocks are kept, each invalid one is reported back as `REFUSED: <reason>`), and `reportInsufficientTime(reason)`, which sets the plan's `shortfall` (AC6). The agent's final reply is the plan's `summary`. *(decided 2026-10-14)*
12. Priority (AC4) is computed in code: score = (6 − understanding level, unknown counts as 3) × Σ over the topic's deadlines still due of (weight ÷ days until due, at least 1 day). Topics without a deadline still due score 0. `getTopics` lists topics highest score first, and the prompt tells the agent to give higher-ranked topics more and earlier blocks. *(decided 2026-10-14)*
13. If the LLM fails (quota, network, missing key) → 503 and nothing is saved, like FR-07 AC12. Planner starts without a key. *(decided 2026-10-14)*
14. A block keeps the topic name and course code it was planned with (so it still reads well if the topic is deleted later) and has a status: `PLANNED`, `STARTED`, `COMPLETED` or `MISSED` (FR-12 AC12). *(decided 2026-10-14)*
15. `reportInsufficientTime` is refused while the plan has no blocks and an availability slot later in the week is still free: the agent must first use the time there is for the highest-ranked topics. *(found in the curl test and decided 2026-10-14: the LLM reported a shortfall and left a free slot empty)*

## FR-05 — Start and end a focus session
**Story:** As a student, I want to start and stop a timed focus session, so my study time is tracked.
**Relevance:** O3 — tracking is the basis of feedback and habit building.
**Acceptance criteria**
1. A student can start a session with optional time block ID, optional topic ID, planned minutes (10–180) and an allowed-sites list.
2. A student can have only one active session at a time (second start returns 409 Conflict).
3. Ending a session records the actual duration.
4. Starting publishes `FocusSessionStarted`; ending publishes `FocusSessionEnded`.
5. The actual duration is computed by the server as the exact time between start and end in minutes and seconds (fractions of a second are dropped). The response shows it as minutes and seconds, e.g. `25m 59s`; the event carries the total in seconds (`actualSeconds`). Ending before or after the planned time is allowed. *(decided 2026-10-13)*
6. Ending an already-ended session returns 409 Conflict. Ending a session that belongs to another user returns 404 Not Found. *(decided 2026-10-13)*
7. A session ends only when the student ends it; there is no automatic end when planned minutes run out. *(decided 2026-10-13)*
8. Time block ID and topic ID are stored as given; Focus does not check that they exist (it owns neither). *(decided 2026-10-13)*

## FR-06 — Log a distraction
**Story:** As a student, I want to quickly log when I get distracted, so I can see my patterns and get support.
**Relevance:** O3 — awareness of distraction is the first step to reducing it.
**Acceptance criteria**
1. A distraction can only be logged during an active session.
2. It has a type (PHONE, SOCIAL_MEDIA, OTHER_SITE, THOUGHT, OTHER), optional site and optional note.
3. If a site is given and is not in the session's allowed-sites list, the distraction is marked off-policy.
4. Logging publishes `DistractionLogged`.
5. `POST /sessions/{sessionId}/distractions` with `{type, site, note}`. An ended session → 409; another student's or an unknown session → 404. The time is the server's clock (no backdating). *(decided 2026-10-14)*
6. `type` is case-insensitive (`phone` = `PHONE`); missing or unknown → 400. Site (optional) is at most 200 characters and note (optional) at most 500, both trimmed; blank counts as not given. The note stays in Focus and is not in the event. *(decided 2026-10-14)*
7. Sites are compared as hosts: lower-cased, without `http://` / `https://`, `www.`, port or path; allowed sites are cleaned the same way when the session starts. A subdomain of an allowed site is allowed (`docs.google.com` is allowed by `google.com`). With no site the distraction is not off-policy; with a site and an empty allowed-sites list it is off-policy. *(decided 2026-10-14)*
8. `GET /sessions/{id}` also lists the session's distractions (type, site, note, off-policy, logged at). *(decided 2026-10-14)*

## FR-07 — Socratic AI tutor (Agentic AI)
**Story:** As a student, I want an AI tutor that guides me with questions and hints instead of giving me the answer, so I actually learn.
**Relevance:** O2 — counters passive "skip to the answer" AI use.
**Acceptance criteria**
1. A student starts a tutoring session with a topic ID and a question, then continues with messages.
2. The tutor's first response is a guiding question or hint, never a full solution.
3. The full solution can only be produced through a `revealSolution` tool, which code-enforces that at least 2 student attempts have been recorded in this session; otherwise the tool refuses and the tutor gives another hint.
4. The tutor grounds its help in the topic's material when material exists.
5. At the end, the tutor asks a check-understanding question and records an understanding level (1–5), publishing `UnderstandingRecorded`.
6. Endpoints: `POST /tutoring-sessions` with `{topicId, question}` → 201 with the tutor's first reply; `POST /tutoring-sessions/{id}/messages` with `{text}`; `POST /tutoring-sessions/{id}/end`; `GET /tutoring-sessions/{id}` shows status (`ACTIVE`, `CHECKING`, `ENDED`), attempts, whether the solution was revealed, the level and the transcript. *(decided 2026-10-14)*
7. **Attempts:** the tutor records an attempt with a `recordAttempt` tool when the student's message is a real try at answering. Code allows at most one attempt per student message and none for the opening question, so the agent cannot invent attempts. *(decided 2026-10-14)*
8. **Solution:** `revealSolution` refuses while fewer than 2 attempts are recorded. When allowed, it asks a separate "solver" LLM call for the solution and returns it; the solver is reachable only through this tool. The tutor's instructions forbid writing a solution itself (AC2); the tutor's free text cannot be checked by code — a known limit. Attempts and the solution are only possible while `ACTIVE`. *(decided 2026-10-14)*
9. **Grounding:** a `searchMaterial(query)` tool returns up to 3 paragraphs from the topic's materials that share the most words with the query (plain Java, no embeddings). With no material, it says so and the tutor helps from general knowledge. *(decided 2026-10-14)* The 3 paragraphs closest to the student's question are also always put in the tutor's instructions, so grounding does not depend on the LLM choosing to search. *(found in the curl test and decided 2026-10-14)*
10. **Ending:** `POST /tutoring-sessions/{id}/end` moves an `ACTIVE` session to `CHECKING`, adds the student line "I'd like to end the session now." to the transcript (so it keeps alternating student/tutor), and the tutor replies with one check-understanding question. The student answers with a normal message; the tutor grades it with a `recordUnderstanding(level)` tool (code checks 1–5, only in `CHECKING`, only once, and only when the newest message is the student's answer to the check question — never straight after `/end`), which ends the session and publishes `UnderstandingRecorded`. If the tutor does not record a level, the session stays `CHECKING` and the student can answer again. `/end` on a session that is not `ACTIVE` → 409; a message to an `ENDED` session → 409. *(decided 2026-10-14)*
11. Question and messages are 1–2,000 characters (trimmed), else 400. An unknown topic or session, or another student's (`X-User-Id`) → 404. *(decided 2026-10-14)*
12. If the LLM fails (quota, network, missing key) → 503 and nothing from that request is saved, so the student can resend the same message. Tutor starts without a key; only tutoring needs it. *(decided 2026-10-14)*
13. When Tutor receives `TopicDeleted`, it also removes that topic's tutoring sessions (no event), like materials (FR-02 AC7). *(decided 2026-10-14)*
14. Planner keeps the latest understanding level of each of its topics from `UnderstandingRecorded` (the event with the latest `occurredAt` wins; one for an unknown topic is ignored), and `GET /courses/{id}` shows `understandingLevel` per topic (`null` until recorded). FR-04's planning agent reads it. *(decided 2026-10-14)*

## FR-08 — Friends, study rooms and sharing settings
**Story:** As a student, I want to connect with friends and join study rooms, so I don't study alone.
**Relevance:** O4 — social accountability.
**Acceptance criteria**
1. A student can send a friend request; the other user can accept it.
2. A student can create a study room and others can join it by room ID.
3. Each student has a sharing setting, `false` by default; presence is visible to others only when `true`.
4. There is no user registry: any non-blank user ID is a valid user (see `01-domain.md` §5, no authentication). *(decided 2026-10-14)*
5. Friend request to yourself → 400. Request when already friends, or when a pending request exists in either direction → 409. *(decided 2026-10-14)*
6. Only the recipient can accept a request; anyone else (including the sender) gets 404. Accepting an already-accepted request → 409. Friendship is mutual once accepted. *(decided 2026-10-14)*
7. Creating a room (name 1–60 characters) makes the creator its first member. Joining a room you are already in → 409; unknown room → 404. There is no "leave room". *(decided 2026-10-14)*

## FR-09 — Live "studying now" board (Stream processing)
**Story:** As a student, I want to see which friends are studying right now, so I feel accompanied and motivated to start.
**Relevance:** O4 — the "study together" effect.
**Acceptance criteria**
1. A student can list friends currently in a focus session, with topic (if any) and minutes elapsed.
2. A student can see how many members of a study room are currently studying.
3. The board reflects session start/end within a few seconds.
4. Implemented as a Kafka Streams stateful aggregation over `focus.sessions`, queried via interactive queries.
5. Users with sharing off never appear. Presence is stored for every user; the sharing filter is applied at query time against Social's `SharingSetting`, so turning sharing off takes effect immediately. *(decided 2026-10-13)*
6. The room count includes **every** member currently studying, whatever their sharing setting: it is an anonymous number, so no one "appears". *(decided 2026-10-14)*
7. Only members of a room can see its count; non-members get 404. *(decided 2026-10-14)*
8. Elapsed time is computed at query time from the session's `startedAt` and shown as minutes and seconds (`12m 5s`, same format as FR-05) plus the total in seconds. *(decided 2026-10-14)*
9. The board shows the `topicId` (or null), not the topic name: Social does not consume `planner.topics`. *(decided 2026-10-14)*
10. The viewer's own sharing setting does not limit what they can see; it only controls whether others see them. *(decided 2026-10-14)*

## FR-10 — Real-time focus progress and streak (Stream processing)
**Story:** As a student, I want to see my focus minutes today, this week and my streak, so I can see that I'm improving.
**Relevance:** O3 — visible progress reinforces habit.
**Acceptance criteria**
1. A student can view focus minutes for today and for each of the last 7 days. Each day is shown as minutes and seconds (e.g. `24m 59s`, same format as FR-05) plus the total in seconds. "Last 7 days" are the seven Sydney days ending today. *(decided 2026-10-13)*
2. A student can view their current streak (consecutive days with ≥ 25 focus minutes). A "day" is a calendar day in `Australia/Sydney`. *(decided 2026-10-13)*
3. Values update within a few seconds of a session ending.
4. Implemented as a Kafka Streams windowed aggregation (1-day windows aligned to `Australia/Sydney` midnight) over `FocusSessionEnded` events.
5. A session's whole duration counts toward the Sydney day on which it **started** (event time = `startedAt`), even if it ends after midnight. *(decided 2026-10-13)*
6. A `FocusSessionEnded` that arrives more than 1 day after the end of its start day (grace period) is not counted. *(decided 2026-10-13)*
7. The streak counts back from today if today already has ≥ 25 minutes, otherwise from yesterday. Today does not break the streak until the day is over. The streak has no fixed cap; it is limited only by how long the state store keeps daily totals (retention 10 years). *(decided 2026-10-13)*
8. `GET /progress/{userId}` takes the user from the path and does not check `X-User-Id`, because the Planner service calls it on the student's behalf. *(decided 2026-10-13)*
9. Stream logic is unit-tested via pure domain classes (day bucketing, streak); the topology itself is verified end to end with curl. `kafka-streams-test-utils` is not used because it is not an approved dependency. *(decided 2026-10-13)*

## FR-11 — Personal goals and value statements
**Story:** As a student, I want to write down my goals and the ideas that matter to me, so the system can remind me why I'm studying.
**Relevance:** O5 — intrinsic motivation.
**Acceptance criteria**
1. A student can create goals (title, "why" statement, optional target date) and mark them achieved.
2. A student can create value statements (short text, max 280 characters).
3. Goal title 1–100 characters and "why" statement 1–500 characters, both required (trimmed). The optional target date is a date that may not be before today in Australia/Sydney → 400. *(decided 2026-10-14)*
4. Marking a goal achieved records when; marking it again → 409. There is no "un-achieve". *(decided 2026-10-14)*
5. Value statement text is 1–280 characters after trimming. *(decided 2026-10-14)*
6. Goals and value statements can be listed and deleted. Another student's goal or value statement → 404. *(decided 2026-10-14)*

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
8. The 30-minute windows are fixed (tumbling) windows aligned to the clock (10:00–10:30, 10:30–11:00, …), counted per student across all their sessions, with event time = when the distraction was logged. The alert is published once, when a window reaches its 3rd distraction (`distractionCount` = 3); a 4th or later distraction in the same window publishes nothing. Distractions that straddle a window boundary are not combined (accepted trade-off for simplicity). *(decided 2026-10-14)*
9. The reminder quotes one item picked at random from the student's not-yet-achieved goals (title and "why") and value statements. With nothing to quote, the reminder still gets a general encouraging message plus a tip to add a goal. A repeated `DistractionSpikeDetected` for the same student and `windowStart` creates no second reminder. *(decided 2026-10-14)*
10. `GET /reminders` lists the student's unread reminders, newest first; `POST /reminders/{id}/read` marks one read and records when. Marking it read again → 409; another student's reminder → 404. *(decided 2026-10-14)*
11. Scope: AC3 and AC4 need time blocks, which FR-04 creates, so they are built in step 9 with the planning agent. AC5 (optional) and AC7 (stretch) are not built for now; revisit after step 10. *(decided 2026-10-14)*
12. Planner reads both `FocusSessionStarted` and `FocusSessionEnded` from `focus.sessions`. A started session with a `timeBlockId` marks a `PLANNED` block `STARTED`; an ended one marks the block `COMPLETED` from any status (late is fine). An unknown block, or another student's block, is ignored. A block is **missed** when its end has passed and no session was started for it (still `PLANNED`) — so a session that runs past the block's end is not "missed". The job marks it `MISSED` and creates one `MISSED_BLOCK` reminder that quotes a goal or value statement as in AC9. *(decided 2026-10-14)*

---

## Non-functional notes
- Each service owns its own H2 database; no shared tables.
- Services communicate asynchronously via Kafka, plus a small number of REST calls (documented in `03-bounded-contexts.md`).
- No secrets in the repository; the LLM API key is read from an environment variable.
