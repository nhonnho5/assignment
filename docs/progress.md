# Progress

Single source of truth for where the build is. Claude ticks items when a step is verified and committed.

## Before coding
- [x] Git repo, `.gitignore`, `.gitattributes`, `.claude/` settings, hooks and skills
- [ ] Decide LLM provider (`09-conventions.md` open decision)
- [ ] Confirm `spring-cloud-stream-binder-kafka-streams` with coordinator (blocks steps 3, 4, 7)
- [ ] Grill session: resolve open questions below and write answers into docs

## Build order (from CLAUDE.md)
- [ ] 1. Skeleton — parent pom, 4 modules on 8081–8084, docker-compose Kafka
- [ ] 2. Slice A — FR-05 focus sessions → `focus.sessions` → Social logs
- [ ] 3. FR-10 progress stream + `GET /progress/{userId}`
- [ ] 4. FR-08, FR-09 social + presence store
- [ ] 5. FR-01, FR-03, FR-11 planner + `TopicCreated` → Tutor
- [ ] 6. FR-02 materials + `MaterialAdded` → Planner
- [ ] 7. FR-06, FR-12 distractions, spike stream, reminders
- [ ] 8. FR-07 Socratic tutor agent
- [ ] 9. FR-04 planning agent
- [ ] 10. Postman collection + README

## Open questions (to resolve in a grill session)
1. FR-10 streaks: what time zone defines a "day" — UTC or Australia/Sydney?
2. FR-12.3 missed block: is `@Scheduled` (part of Spring, no new dependency) the trigger?
3. `DomainException` → HTTP status: how to tell 400 vs 404 vs 409?
4. Planner consumes `FocusSessionEnded` "to mark time blocks completed" — no FR covers it. Add an acceptance criterion or drop it?
5. FR-09: sharing settings live in Social's DB, presence in a Streams store — filter at query time?
6. FR-04: is a week start always a Monday?
