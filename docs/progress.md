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
- [ ] 9. FR-04 planning agent — **also FR-12 AC3/AC4** (time block completed on `FocusSessionEnded`, missed-block reminders via `@Scheduled`), moved here because time blocks only exist from FR-04
- [ ] 10. Postman collection + README

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

## Open questions
None right now. Run `/grill-me` before each slice to catch new ones.

## Reminders for the user
- [x] Working alone instead of a group of 3: approved by the professor (confirmed by the user 2026-10-14).
- [ ] **After step 10 (code done):** go back to the subject spec (`docs/CSCI318 Project Specification Spring 2026.pdf`, local only, git-ignored) and check every deliverable against it. Known gaps so far:
  - UML diagrams: use cases + activity diagram per FR, domain model, architecture, database design (AI-assisted artefacts in the repo, origin labelled).
  - README sample inputs.
  - LangChain4j dependency (comes with step 8).
  - Fill in the "My rationale" column in `docs/ai-log/`. The report itself must be written by the user, not by AI.
- [x] Coordinator approved a PDF library for FR-02 material upload (2026-10-14) — Apache PDFBox chosen.
- [x] Remote `origin` = https://github.com/nhonnho5/assignment.git added 2026-10-14; the user pushes (Claude never pushes).
- [x] ~~**After step 5 is committed:** remind the user to set up a remote and push (added 2026-10-14). The repo has no remote yet. The user creates a private GitHub/GitLab repo and sends the URL; then either Claude adds `origin` and pushes (after the user approves changing the "never `git push`" rule in `CLAUDE.md`), or the user runs `git remote add origin <url>` and `git push -u origin main`.~~
