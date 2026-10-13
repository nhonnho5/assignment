# Progress

Single source of truth for where the build is. Claude ticks items when a step is verified and committed.

## Before coding
- [x] Git repo, `.gitignore`, `.gitattributes`, `.claude/` settings, hooks and skills
- [x] Decide LLM provider — Google Gemini
- [x] Confirm `spring-cloud-stream-binder-kafka-streams` with coordinator — approved 2026-10-13
- [x] Grill session 1: six open questions resolved and written into `02-requirements.md` / `09-conventions.md`
- [ ] Get a Gemini API key (aistudio.google.com) and put it in `.env` as `LLM_API_KEY` (needed by step 8)

## Build order (from CLAUDE.md)
- [x] 1. Skeleton — parent pom, 4 modules on 8081–8084, docker-compose Kafka
- [x] 2. Slice A — FR-05 focus sessions → `focus.sessions` → Social logs
- [x] 3. FR-10 progress stream + `GET /progress/{userId}`
- [x] 4. FR-08, FR-09 social + presence store
- [x] 5. FR-01, FR-03, FR-11 planner + `TopicCreated` → Tutor
- [ ] 6. FR-02 materials + `MaterialAdded` → Planner
- [ ] 7. FR-06, FR-12 distractions, spike stream, reminders
- [ ] 8. FR-07 Socratic tutor agent
- [ ] 9. FR-04 planning agent
- [ ] 10. Postman collection + README

## Step 5 plan (approved 2026-10-14, built and verified with curl 2026-10-14)
Small additions made while building: Planner accepts `"monday"` as well as `"MONDAY"`. A fractional weight such as `12.5` is not rejected; it is stored as `12` (decided 2026-10-14).

Code style: shortest readable code (see `CLAUDE.md` hard rules). Grill done; decisions are in `02-requirements.md` (FR-01 AC5–10, FR-03 AC4–5, FR-11 AC3–6) and `05-events.md` (`TopicDeleted`). Build exactly this:

**planner-service**
- domain: `Course` (aggregate root; holds `Topic` and `Deadline` entities; registers `TopicCreated`/`TopicDeleted`), `CourseCode`, `Weight` (value objects), `WeeklyAvailability` (aggregate root, one per user) with `AvailabilitySlot` (entity), `Goal`, `ValueStatement` (aggregate roots), `DomainEvent`, `EventPublisher`, `TopicCreated`, `TopicDeleted`, `StudyCalendar` (Sydney zone, Sydney local → instant), `Text` (one shared trim + length check used by every name/title), `DomainException`/`NotFoundException`/`ConflictException`, repository interfaces for Course, WeeklyAvailability, Goal, ValueStatement.
- service: `CourseService` (duplicate code per user → 409; others' course → 404; save then publish), `AvailabilityService`, `MotivationService` (goals + value statements).
- infrastructure: JPA adapters + Spring Data repos (EAGER `Set`s, `open-in-view: false`), `StreamBridgeEventPublisher` (binding `plannerTopics-out-0` → `planner.topics`, key = userId), `TimeConfig` (`Clock.tickMillis(UTC)`).
- presentation: `CourseController`, `AvailabilityController`, `MotivationController` (goals + values), `PlannerDtos` (all DTOs in one file), `ApiExceptionHandler`.
- endpoints: `POST/GET /courses`, `GET/DELETE /courses/{id}`, `POST /courses/{id}/topics`, `DELETE /courses/{id}/topics/{topicId}`, `POST /courses/{id}/deadlines`, `DELETE /courses/{id}/deadlines/{deadlineId}`, `POST /availability/slots`, `GET /availability`, `DELETE /availability/slots/{id}`, `POST/GET /goals`, `POST /goals/{id}/achieve`, `DELETE /goals/{id}`, `POST/GET /values`, `DELETE /values/{id}`.

**tutor-service**
- domain `TopicReference` (read model: topicId, userId, courseId, courseCode, topicName) + repository; service `TopicCatalogService` (idempotent: repeated `TopicCreated` → no duplicate, unknown `TopicDeleted` → ignored); infrastructure `Consumer` bean `plannerTopics` on `planner.topics` (group `tutor-service`, `resetOffsets: true`, `startOffset: earliest`, logs each event) + JPA adapter; presentation `GET /topics` (by `X-User-Id`).

**Tests first:** `CourseTest` (FR01_courseCodeIsTrimmedAndUpperCased, FR01_addingTopicRegistersTopicCreated, FR01_duplicateTopicNameIsConflict, FR01_deadlineInPastIsRejected, FR01_deadlineWeightMustBe1To100, FR01_deadlineTopicsMustBelongToCourse, FR01_totalWeightCannotExceed100, FR01_cannotDeleteTopicUsedByDeadline, FR01_deletingTopicRegistersTopicDeleted, FR01_deletingCourseRegistersTopicDeletedForEachTopic); `WeeklyAvailabilityTest` (FR03_slotMustBeAtLeast25Minutes, FR03_endMustBeAfterStart, FR03_timesMustBeWholeMinutes, FR03_overlappingSlotIsConflict, FR03_touchingSlotsAreAllowed, FR03_sameTimeOnOtherDayIsAllowed, FR03_removingUnknownSlotIsNotFound); `GoalTest`/`ValueStatementTest` (FR11_goalNeedsTitleAndWhy, FR11_targetDateCannotBeBeforeToday, FR11_achievingTwiceIsConflict, FR11_valueStatementMax280Characters); service tests with in-memory fakes (FR01_duplicateCourseCodeIsConflict, FR01_otherStudentsCourseIsNotFound, FR01_eventsPublishedOnlyAfterSave; Tutor FR01_topicCreatedStoredOnce, FR01_topicDeletedRemovesTopic).

**Verify:** Kafka + Planner + Tutor; create course/topics/deadline via curl; Tutor log line + `GET /topics` on 8082; delete a topic → gone from Tutor.

## Open questions
None right now. Run `/grill-me` before each slice to catch new ones.

## Reminders for the user
- [x] Working alone instead of a group of 3: approved by the professor (confirmed by the user 2026-10-14).
- [ ] **After step 10 (code done):** go back to the subject spec (`docs/CSCI318 Project Specification Spring 2026.pdf`, local only, git-ignored) and check every deliverable against it. Known gaps so far:
  - UML diagrams: use cases + activity diagram per FR, domain model, architecture, database design (AI-assisted artefacts in the repo, origin labelled).
  - README sample inputs.
  - LangChain4j dependency (comes with step 8).
  - Fill in the "My rationale" column in `docs/ai-log/`. The report itself must be written by the user, not by AI.
- [ ] **After step 5 is committed:** remind the user to set up a remote and push (added 2026-10-14). The repo has no remote yet. The user creates a private GitHub/GitLab repo and sends the URL; then either Claude adds `origin` and pushes (after the user approves changing the "never `git push`" rule in `CLAUDE.md`), or the user runs `git remote add origin <url>` and `git push -u origin main`.
