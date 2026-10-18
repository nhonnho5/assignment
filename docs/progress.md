# Progress

Single source of truth for where the build is. Claude ticks items when a step is verified and committed.

## Before coding
- [x] Git repo, `.gitignore`, `.gitattributes`, `.claude/` settings, hooks and skills
- [x] Decide LLM provider — Google Gemini
- [x] Confirm `spring-cloud-stream-binder-kafka-streams` with coordinator — approved 2026-10-13
- [x] Grill session 1: six open questions resolved and written into `02-requirements.md` / `09-conventions.md`
- [x] Get a Gemini API key (aistudio.google.com) and put it in `.env` as `LLM_API_KEY` (needed by step 8) — present 2026-10-14 (checked without reading the value); first real use in step 8

## Build order (from CLAUDE.md)
- [x] 1. Skeleton — parent pom, 4 modules on 8081–8084, docker-compose Kafka
- [x] 2. Slice A — FR-05 focus sessions → `focus.sessions` → Social logs
- [x] 3. FR-10 progress stream + `GET /progress/{userId}`
- [x] 4. FR-08, FR-09 social + presence store
- [x] 5. FR-01, FR-03, FR-11 planner + `TopicCreated` → Tutor
- [x] 6. FR-02 materials + `MaterialAdded` → Planner
- [x] 7. FR-06, FR-12 distractions, spike stream, reminders
- [x] 8. FR-07 Socratic tutor agent
- [x] 9. FR-04 planning agent — **also FR-12 AC3/AC4** (time block completed on `FocusSessionEnded`, missed-block reminders via `@Scheduled`), moved here because time blocks only exist from FR-04
- [x] 10. Postman collection + README

## Step 10 (Postman + README) — verified 2026-10-14
- **Postman:** `postman/StudyAnchor.postman_collection.json` (folders 0 Setup, then FR-01 → FR-04/12; every request has tests; new users each run) + `postman/sample-material.md`. Added: topic delete reaches Tutor (FR-01 AC9/10), goal delete and another student's → 404 (FR-11 AC6), accept twice → 409 (FR-08 AC6), plan with no availability → 400 (FR-04 AC9). newman run against real Gemini: 97 requests, 141 assertions, 0 failed.
- **README.md:** prerequisites, `.env`, run steps, Postman/newman, one curl sample per FR (non-LLM ones run for real), error codes, troubleshooting.
- **`.env.example` deleted** (user's decision); `!.env.example` removed from `.gitignore`.
- **Live FR-12 AC4 test (carried over from step 9):** block 08:19–08:44 today, no session → `MISSED` at 08:44:24 and one `MISSED_BLOCK` reminder; still one reminder 75 s later.

## Step 9 (FR-04, FR-12 AC3/AC4) — built and verified with curl against real Gemini 2026-10-14 (live missed-block test done in step 10)
Decisions are in `02-requirements.md` FR-04 AC8–15 and FR-12 AC12.

- **planner-service domain:** `StudyPlan` (aggregate root, one per student per week; every block rule, the shortfall rule, block status), `TimeBlock` (entity: Sydney date/times, topic name + course code copy, rationale, `PLANNED → STARTED → COMPLETED` or `PLANNED → MISSED`), `TopicPriority` (domain service: (6 − level) × Σ weight ÷ days), ports `PlanningAgent` and `StudyPlanRepository`, `AgentUnavailableException` (503). `Reminder` has a new kind `MISSED_BLOCK` (`missed:<blockId>`, once per block); `WeeklyAvailability.covers`.
- **service:** `StudyPlanService` (`generate` with the 409 "send `replace: true`" reminder, `get`, session started/ended, `markMissedBlocks`); `ReminderService.onMissedBlock` shares the goal/value quote picker with spike reminders.
- **infrastructure:** `PlanningTools` (`getDeadlines`, `getAvailability`, `getTopics`, `getFocusStats`, `addBlocks(list)`, `reportInsufficientTime`), `GeminiPlanningAgent`, `FocusProgressClient` (`RestClient` → Focus `GET /progress/{userId}`), `FocusSessionsConsumer` on `focus.sessions` (group `planner-service`), `MissedBlockScheduler` (`@Scheduled` every minute), JPA adapter. Planner now loads `.env` and has `llm.*` and `focus.base-url`.
- **presentation:** `StudyPlanController` (`POST /plans`, `GET /plans/{weekStart}`), `PlanResponse` / `BlockResponse`, 503 mapping.
- **Tests:** `StudyPlanTest` (13), `TopicPriorityTest` (3), `PlanningToolsTest` (6, no LLM), `StudyPlanServiceTest` (6), `ReminderTest` +1.
- **Found in the curl test and fixed:** the LLM reported "not enough time" and left a free slot empty, even with a clearer prompt; `reportShortfall` is now refused while the plan is empty and a later slot is still free (AC15). The log shows the refusal and the agent's fix.

## Step 8 (FR-07) — built and verified with curl against real Gemini 2026-10-14
Decisions are in `02-requirements.md` FR-07 AC6–14; LangChain4j 1.22.0 and the model setting in `09-conventions.md`.

- **tutor-service:** `TutoringSession` (aggregate root: transcript, attempts, `ACTIVE → CHECKING → ENDED`, all tool rules), `Message` (value object; a student message can be marked as an attempt), `SessionStatus`, `UnderstandingRecorded` → `tutor.understanding`, `MaterialSearch` (domain service, word-overlap paragraphs), ports `TutorAgent` and `TutoringSessionRepository`, `ConflictException` (409), `TutorUnavailableException` (503). `TutoringService` (LLM failure rolls the request back). `TutoringTools` (`searchMaterial`, `recordAttempt`, `revealSolution` → separate `Solver` LLM call, `recordUnderstanding`; refusals go back to the LLM as "REFUSED: …"), `GeminiTutorAgent` (rebuilds the chat from the transcript each turn, always includes the 3 closest material paragraphs). `TutoringController`: `POST /tutoring-sessions`, `POST /{id}/messages`, `POST /{id}/end`, `GET /{id}`. `TopicDeleted` also removes sessions. `.env` is loaded through `spring.config.import`.
- **planner-service:** `Topic` keeps the latest understanding level (by `occurredAt`); `TutorUnderstandingConsumer` on `tutor.understanding`; `GET /courses/{id}` shows `understandingLevel`.
- **Tests:** `TutoringSessionTest` (12), `MaterialSearchTest` (4), `TutoringToolsTest` (5, no LLM), `TutoringServiceTest` (5), `TopicCatalogServiceTest` +1, `CourseTest` +1, `CourseServiceTest` +1.
- **Found in the curl test and fixed:** (1) Gemini 3 needs its "thought signature" sent back after a tool call; (2) the LLM graded straight after `/end` without asking the check question; the domain now refuses that (`FR07_understandingOnlyAfterStudentAnsweredCheckQuestion`); (3) the LLM did not always call `searchMaterial`, so the closest paragraphs are now always in its instructions.
- **Model:** default is now `gemini-3.5-flash-lite` (verified); `gemini-3.8-flash` allowed only 5 requests/minute on the free tier and was overloaded during the test.

## Step 7 (FR-06, FR-12) — built and verified with curl 2026-10-14
Decisions are in `02-requirements.md` FR-06 AC5–8 and FR-12 AC8–11; `05-events.md` `DistractionSpikeDetected`.

- **focus-service:** `FocusSession.logDistraction` (aggregate root) with `Distraction` (value object, `@Embeddable` record), `DistractionType`, `Site` (host cleanup + subdomain match; allowed sites are now cleaned the same way at start), events `DistractionLogged` → `focus.distractions` and `DistractionSpikeDetected` (holds the 30 min / 3 rule). `DistractionSpikeTopology`: per-user 30-min tumbling count (caching off, 1-min grace) → `focus.alerts`. Kafka record timestamp = event `occurredAt`. `POST /sessions/{id}/distractions`; `GET /sessions/{id}` lists distractions. Each Streams function has its own `applicationId`.
- **planner-service:** `Reminder` (aggregate root, `triggerKey` = `spike:<windowStart>` unique per user), `Goal.quote()` / `ValueStatement.quote()`, `ReminderService`, `FocusAlertsConsumer` on `focus.alerts` (group `planner-service`), `ReminderController` (`GET /reminders`, `POST /reminders/{id}/read`).
- **Tests:** `DistractionTest` (9), `FocusSessionServiceTest` +2, `ReminderTest` (3), `ReminderServiceTest` (3).
- **Not built:** FR-12 AC5 (random reminders, optional) and AC7 (LLM text, stretch) — revisit after step 10.
- **FR-10 bug found and fixed (separate commit):** the progress stream died ~30 s after a session ended because Kafka 4 rejects changelog records stamped 11 h ahead (Sydney timeline). Its changelog topic now allows up to 1 day ahead. After pulling this, run `docker compose down` and `docker compose up -d` once so the topic is recreated.

## Step 6 (FR-02) — built and verified with curl 2026-10-14
Decisions are in `02-requirements.md` FR-02 AC4–8; PDFBox approved in `09-conventions.md`.

- **tutor-service:** `Material` (aggregate root, registers `MaterialAdded`), `MaterialText` port + `UploadedFileText` adapter (.txt/.md as UTF-8, .pdf via PDFBox), `MaterialService`, `MaterialController` (`POST/GET /topics/{topicId}/materials`, `GET /materials/{id}`), `ApiExceptionHandler`, `StreamBridgeEventPublisher` → `tutor.materials`. `TopicDeleted` also removes the topic's materials. Upload size limit 20 MB (the 50,000-character text limit is checked after extraction).
- **planner-service:** `Topic` keeps material IDs (`materialCount` in `GET /courses/{id}`); `TutorMaterialsConsumer` on `tutor.materials` (group `planner-service`).
- **Tests:** `MaterialTest` (6), `UploadedFileTextTest` (4), `MaterialServiceTest` (4), `TopicCatalogServiceTest` +1, `CourseTest` +1, `CourseServiceTest` +1.

## Session 2026-10-19 (submission day): run guide, code guide, report outline
Goals set by the user, in order. Claude ticks each item when it is done.

**Goal 1: anyone can run it (IMPORTANT)**
- [x] `docs/guide/01-run-it.md`: Windows step-by-step run guide, with expected output for each step
- [x] Claude tested the guide from scratch (Kafka down → up, `mvn package` BUILD SUCCESS in 2:03, 4 services up, browser check, newman 97 requests / 141 assertions / 0 failed)
- [ ] ~~The user ran it alone with no AI and it worked~~ Skipped by the user 2026-10-19 to save time; do it before the Week 12 presentation.

**Goal 2: documentation (EXTREMELY IMPORTANT)**
- [ ] Code explainer guide in `docs/guide/`: [x] 02 big picture · [x] 03 inside a service · [x] 04 DDD patterns · [x] 05 Kafka and streams · [x] 06 AI agents · [ ] 07 every FR
- [x] Report outline (`docs/report-outline.md`): headings, what goes in each section, which repo artefact or code snippet to use (the user writes all prose)

## Open questions
None right now. Run `/grill-me` before each slice to catch new ones.

## After submission (Mon 2026-10-19, 5pm): demo page for the Week 12 presentation
Decided 2026-10-17: the user approves an exception to "no frontend" for the presentation only. Build it after the report is submitted, so the submitted repo stays as is.
- One static HTML page with plain JS (no framework, no new dependency) and a small CORS setting in each service.
- Pick a demo user, one "seed demo data" button, then buttons for: start/end focus session, log a distraction, progress, "studying now" board, reminders, AI tutor chat, generate weekly plan.

## Reminders for the user
- [x] ~~**At the start of step 10: run the live missed-block test (FR-12 AC4).**~~ Done 2026-10-14, passed (see Step 10). Closed step 9 without it (the user decided 2026-10-14; Planner was restarted mid-test). It is covered by unit tests (`FR12_blockIsMarkedMissedOnceAfterItEnds`, `FR12_missedBlockCreatesOneReminderQuotingAGoal`, `FR12_startedBlockGetsNoMissedReminder`). To test: add a 25-minute availability slot today that starts a few minutes from now, plan the current week, start no focus session, and about a minute after the block ends check `GET /plans/{weekStart}` (`"status":"MISSED"`) and `GET /reminders` (one `MISSED_BLOCK` reminder; still one a minute later).
- [x] Working alone instead of a group of 3: approved by the professor (confirmed by the user 2026-10-14).
- [x] **After step 10 (code done):** spec check done 2026-10-14 — see "Spec check" below.

## Spec check (2026-10-14, against `docs/CSCI318 Project Specification Spring 2026.pdf`)
**Done in the repo**
- [x] 4 bounded contexts, each a microservice (Planner, Tutor, Focus, Social)
- [x] Event-driven communication over Kafka (6 topics, `05-events.md`); REST where needed (Planner → Focus progress)
- [x] Stream processing, ≥ 2 stories: FR-09, FR-10, FR-12 (Kafka Streams)
- [x] Agentic AI, ≥ 2 stories: FR-04, FR-07 (LangChain4j + Gemini, tool rules in code)
- [x] Mandatory stack: JDK 21, Spring Boot, Spring Web, Spring Data JPA, H2, Spring Cloud Stream, LangChain4j, Kafka; extra deps approved by the coordinator
- [x] ≥ 12 functional requirements as user stories with acceptance criteria (`02-requirements.md`)
- [x] Layered packages `presentation / service / domain / infrastructure` in every service
- [x] Data ownership table (`03-bounded-contexts.md`)
- [x] README with configuration/running instructions and sample inputs per FR
- [x] Lightweight client: Postman collection that seeds data and exercises every FR
- [x] AI-use record: `docs/ai-log/`, `CLAUDE.md`, `.claude/skills/`

**Repo artefacts for the report (AI-assisted, origin labelled) — drafted 2026-10-14, PlantUML in `docs/diagrams/` + PNGs; the user still reviews them**
- [x] Use case diagram + one UML activity diagram per FR (Report §2.1) — `docs/diagrams/`
- [x] UML domain model diagram, classes grouped into sub-domains (§2.4)
- [x] Architecture diagram: services, REST vs Kafka, data flows (§3.1)
- [x] Layered architecture diagram per service, mapped to packages (§3.2)
- [x] Database design (tables per service) (§3.4)
- [x] Stream processing queries written out (SQL-like) (§3.5) — `docs/06-stream-queries.md`

**User only (must not be AI-generated)**
- [ ] "My rationale" column in every `docs/ai-log/` file
- [ ] Project report (PDF): cover page with contribution statement, abstract with GitHub link, sections 1–5, Harvard references
- [ ] Final presentation (Week 12)
- [ ] Keep the GitHub repo private until submission; submit report PDF + repo zip to Moodle
- [x] Coordinator approved a PDF library for FR-02 material upload (2026-10-14) — Apache PDFBox chosen.
- [x] Remote `origin` = https://github.com/nhonnho5/assignment.git added 2026-10-14; the user pushes (Claude never pushes).
- [x] ~~**After step 5 is committed:** remind the user to set up a remote and push (added 2026-10-14). The repo has no remote yet. The user creates a private GitHub/GitLab repo and sends the URL; then either Claude adds `origin` and pushes (after the user approves changing the "never `git push`" rule in `CLAUDE.md`), or the user runs `git remote add origin <url>` and `git push -u origin main`.~~
