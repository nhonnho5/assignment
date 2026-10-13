# Progress

Single source of truth for where the build is. Claude ticks items when a step is verified and committed.

## Before coding
- [x] Git repo, `.gitignore`, `.gitattributes`, `.claude/` settings, hooks and skills
- [x] Decide LLM provider — Google Gemini
- [ ] **Confirm `spring-cloud-stream-binder-kafka-streams` with coordinator** (blocks steps 3, 4, 7 — user must ask)
- [x] Grill session 1: six open questions resolved and written into `02-requirements.md` / `09-conventions.md`
- [ ] Get a Gemini API key (aistudio.google.com) and put it in `.env` as `LLM_API_KEY` (needed by step 8)

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

## Open questions
None right now. Run `/grill-me` before each slice to catch new ones.
