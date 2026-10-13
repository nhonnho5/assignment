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
- [ ] 5. FR-01, FR-03, FR-11 planner + `TopicCreated` → Tutor
- [ ] 6. FR-02 materials + `MaterialAdded` → Planner
- [ ] 7. FR-06, FR-12 distractions, spike stream, reminders
- [ ] 8. FR-07 Socratic tutor agent
- [ ] 9. FR-04 planning agent
- [ ] 10. Postman collection + README

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
