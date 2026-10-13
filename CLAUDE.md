# CLAUDE.md — instructions for Claude Code

This repo is StudyAnchor, a CSCI318 (UOW) project: four Spring Boot microservices that help university students (especially students with ADHD) plan, focus and learn.

## Who you work with
- The user does this assignment **alone** — there is no team to split work with or hand off to.
- The user does **not** write code. Claude writes all code; the user reviews it and tests it as a user (curl / Postman). Never ask the user to implement anything (no "Learn by Doing", no `TODO(human)`).
- Make everything easy for one person: give copy-paste commands (one per block), say exactly what to run and what output to expect, keep decisions to short multiple-choice questions with a recommended option, and do routine work (docs updates, progress ticks, ai-log rows) without being asked.

## Read before writing any code
1. `docs/01-domain.md` — problem, scope, glossary (use these terms in code)
2. `docs/02-requirements.md` — FR-01..FR-12 with acceptance criteria
3. `docs/03-bounded-contexts.md` — which service owns what
4. `docs/05-events.md` — Kafka topics and event contracts (exact names and fields)
5. `docs/09-conventions.md` — stack, packages, DDD rules, dependency rule

If code and docs disagree, the docs win. If the docs are unclear or contradictory, **stop and ask** instead of guessing.

## Hard rules
- Do not add any dependency not listed in `docs/09-conventions.md`. Ask first.
- No Lombok, no frontend, no shared code module between services, no shared database.
- Never commit secrets. The LLM key comes from `LLM_API_KEY`.
- No business logic in controllers. Invariants live in the domain.
- Agent business rules are enforced in tool code, not only in prompts.
- Reference FR IDs in Javadoc and commit messages.
- Work in small steps. After each step: the build passes, the services start, and the user is told how to test the step with curl or Postman.
- Test first for domain rules: write a failing JUnit test (named `FRxx_rule`) before the code that satisfies it. Agent tool rules are unit-tested without calling the LLM.
- Never read `.env`. Never `git push` — the user pushes.

## Where we are
`docs/progress.md` tracks the current step and open questions. Read it at the start of every session; tick items only when a step is verified.

## Workflow skills
- `/grill-me` — resolve ambiguities in the docs before coding.
- `/slice [FR-xx|next]` — implement one build-order step with TDD, verify with curl, commit.
- `/ai-log` — record AI proposals and decisions (the user writes the rationale).

## Build order (one slice at a time — do not jump ahead)
1. **Skeleton:** parent `pom.xml`, four service modules that start on ports 8081–8084, `docker-compose.yml` with Kafka. Verify everything starts.
2. **Slice A — FR-05:** Focus sessions start/end, publish to `focus.sessions`; Social consumes and logs the events. (Proves the Kafka pipeline end to end.)
3. **FR-10:** Kafka Streams progress aggregation in Focus + `GET /progress/{userId}`.
4. **FR-08, FR-09:** Social friends, rooms, sharing; presence state store + queries.
5. **FR-01, FR-03, FR-11:** Planner courses/topics/deadlines, availability, goals; `TopicCreated` → Tutor topic read model.
6. **FR-02:** Tutor materials; `MaterialAdded` → Planner.
7. **FR-06, FR-12:** distractions, spike detection stream, Planner reminders.
8. **FR-07:** Socratic tutor agent.
9. **FR-04:** Planning agent.
10. Postman collection (`postman/`) that seeds demo data and exercises every FR; `README.md` with run instructions.

## After each step, also
- Add a short note to `docs/ai-log/` if the user accepted, changed or rejected something you proposed (the user writes the rationale).
- Explain the new classes in plain language: which layer each is in and why, and which DDD pattern it uses. The user is new to Java and must be able to explain the code.
