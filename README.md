# StudyAnchor

StudyAnchor helps university students, especially students with ADHD, plan, focus and learn. It is a CSCI318 (UOW) project built as four Spring Boot microservices that talk through Apache Kafka. There is no frontend: everything is a REST API, demonstrated with the Postman collection in `postman/`.

| Service | Port | What it does | FRs |
|---|---|---|---|
| planner-service | 8081 | Courses, topics, deadlines, availability, goals, AI weekly plan, reminders | FR-01, FR-03, FR-04, FR-11, FR-12 |
| tutor-service | 8082 | Study material, Socratic AI tutor | FR-02, FR-07 |
| focus-service | 8083 | Focus sessions, distractions, progress and streak, spike detection | FR-05, FR-06, FR-10, FR-12 |
| social-service | 8084 | Friends, study rooms, sharing, live "studying now" board | FR-08, FR-09 |

Each service has its own in-memory H2 database (data is lost on restart) and its own copy of the event records; no code or database is shared. Kafka topics and event fields are in [docs/05-events.md](docs/05-events.md).

**New here? Follow the step-by-step Windows guide: [docs/guide/01-run-it.md](docs/guide/01-run-it.md). To understand the code, read the [guide](docs/guide/README.md) (7 short parts).**

## Prerequisites
- JDK 21 and Maven 3.9+
- Docker Desktop (for Kafka)
- A Google Gemini API key (free at aistudio.google.com), only for the two AI features: the tutor (FR-07) and the weekly plan (FR-04). Everything else works without it.
- Postman, or Node.js to run the collection with newman

## Set up
Create a file named `.env` in the project root (next to this README) with your key. It is git-ignored; never commit it.

```
LLM_API_KEY=your-gemini-key
```

Optional: `LLM_MODEL=...` picks another Gemini model (default `gemini-3.5-flash-lite`).

## Run
All commands run from the project root.

1. Start Kafka (and create the topics):

```bash
docker compose up -d
```

2. Build all four services (runs the unit tests too):

```bash
mvn package
```

3. Start each service in its own terminal:

```bash
java -jar planner-service/target/planner-service-0.1.0-SNAPSHOT.jar
```

```bash
java -jar tutor-service/target/tutor-service-0.1.0-SNAPSHOT.jar
```

```bash
java -jar focus-service/target/focus-service-0.1.0-SNAPSHOT.jar
```

```bash
java -jar social-service/target/social-service-0.1.0-SNAPSHOT.jar
```

Each prints `Started ...Application` when ready. To stop everything: Ctrl+C in each terminal, then `docker compose down`.

## Demo with Postman
`postman/StudyAnchor.postman_collection.json` seeds demo data and checks every FR. Folders run in order (0 Setup, then FR-01 → FR-04), and every request has tests.

- **Postman:** newer versions: open the project folder as a local workspace; the YAML collection in `postman/collections/StudyAnchor/` loads automatically. Older versions: Import `postman/StudyAnchor.postman_collection.json` (same requests). Set Settings → General → Working directory to the `postman` folder, so the material upload finds `sample-material.md`. Then run the whole collection with the Collection Runner.
- **Command line:**

```bash
npx newman run postman/StudyAnchor.postman_collection.json --working-dir postman
```

Expected result: 97 requests, 0 failed. Each run makes new users (`alice-xxxx`, `bob-xxxx`), so you can run it again without restarting anything. A few requests wait 2–3 s for Kafka to deliver an event. Folders 10 and 11 call Gemini: a 503 there means the LLM is unavailable or over quota. Wait a minute and run that folder again.

## Sample inputs (curl)
Every request needs the header `X-User-Id` (any non-blank name; there is no login). Times are Australia/Sydney local time. Replace `<...>` with IDs from earlier responses. Shown for bash (Git Bash on Windows).

**FR-01 Courses, topics, deadlines**: Planner tells Tutor about each topic (`TopicCreated`).

```bash
curl -i -X POST localhost:8081/courses -H "X-User-Id: alice" -H "Content-Type: application/json" -d '{"code":"csci318","name":"Software Engineering"}'
```

```bash
curl -i -X POST localhost:8081/courses/<courseId>/topics -H "X-User-Id: alice" -H "Content-Type: application/json" -d '{"name":"Recursion"}'
```

```bash
curl -i -X POST localhost:8081/courses/<courseId>/deadlines -H "X-User-Id: alice" -H "Content-Type: application/json" -d '{"title":"Assignment 2","dueAt":"2026-11-30T23:59","weight":40,"topicIds":["<topicId>"]}'
```

```bash
curl -i localhost:8082/topics -H "X-User-Id: alice"
```

**FR-02 Study material**: upload a .txt, .md or .pdf; Planner counts it (`MaterialAdded`).

```bash
curl -i -X POST localhost:8082/topics/<topicId>/materials -H "X-User-Id: alice" -F "file=@postman/sample-material.md" -F "title=Week 5 notes"
```

```bash
curl -i localhost:8081/courses/<courseId> -H "X-User-Id: alice"
```

**FR-03 Availability**

```bash
curl -i -X POST localhost:8081/availability/slots -H "X-User-Id: alice" -H "Content-Type: application/json" -d '{"day":"MONDAY","start":"09:00","end":"12:00"}'
```

```bash
curl -i localhost:8081/availability -H "X-User-Id: alice"
```

**FR-11 Goals and values**

```bash
curl -i -X POST localhost:8081/goals -H "X-User-Id: alice" -H "Content-Type: application/json" -d '{"title":"Pass CSCI318 with a distinction","why":"I want to prove I can finish what I start."}'
```

```bash
curl -i -X POST localhost:8081/values -H "X-User-Id: alice" -H "Content-Type: application/json" -d '{"text":"Learning is how I take care of my future self."}'
```

```bash
curl -i -X POST localhost:8081/goals/<goalId>/achieve -H "X-User-Id: alice"
```

**FR-04 AI weekly plan** (needs `LLM_API_KEY`): `weekStart` must be a Monday. Send `"replace": true` to plan the same week again.

```bash
curl -i -X POST localhost:8081/plans -H "X-User-Id: alice" -H "Content-Type: application/json" -d '{"weekStart":"<next Monday, e.g. 2026-10-19>"}'
```

```bash
curl -i localhost:8081/plans/<weekStart> -H "X-User-Id: alice"
```

**FR-05 Focus sessions**: `timeBlockId` and `topicId` are optional. Starting or ending a session with a `timeBlockId` marks that block `STARTED` / `COMPLETED` in the plan (FR-12).

```bash
curl -i -X POST localhost:8083/sessions -H "X-User-Id: alice" -H "Content-Type: application/json" -d '{"topicId":"<topicId>","plannedMinutes":25,"allowedSites":["docs.oracle.com"]}'
```

```bash
curl -i -X POST localhost:8083/sessions/<sessionId>/end -H "X-User-Id: alice"
```

**FR-06 Distractions**: only during an active session. Types: `PHONE`, `SOCIAL_MEDIA`, `OTHER_SITE`, `THOUGHT`, `OTHER`.

```bash
curl -i -X POST localhost:8083/sessions/<sessionId>/distractions -H "X-User-Id: alice" -H "Content-Type: application/json" -d '{"type":"OTHER_SITE","site":"https://www.youtube.com/watch","note":"Clicked a video"}'
```

**FR-12 Reminders**: log 3 distractions within one 30-minute window (10:00–10:30, 10:30–11:00, …). Focus's stream detects the spike, and Planner creates a reminder that quotes a goal or value. A planned block that ends with no focus session started for it is marked `MISSED`, and a "missed block" reminder appears within a minute.

```bash
curl -i localhost:8081/reminders -H "X-User-Id: alice"
```

```bash
curl -i -X POST localhost:8081/reminders/<reminderId>/read -H "X-User-Id: alice"
```

**FR-10 Progress and streak**: today, the last 7 days and the streak (days with ≥ 25 focus minutes).

```bash
curl -i localhost:8083/progress/alice
```

**FR-08 Friends, rooms, sharing**

```bash
curl -i -X POST localhost:8084/friends/requests -H "X-User-Id: bob" -H "Content-Type: application/json" -d '{"toUserId":"alice"}'
```

```bash
curl -i -X POST localhost:8084/friends/requests/<requestId>/accept -H "X-User-Id: alice"
```

```bash
curl -i -X PUT localhost:8084/sharing -H "X-User-Id: alice" -H "Content-Type: application/json" -d '{"sharing":true}'
```

```bash
curl -i -X POST localhost:8084/rooms -H "X-User-Id: alice" -H "Content-Type: application/json" -d '{"name":"CSCI318 study group"}'
```

```bash
curl -i -X POST localhost:8084/rooms/<roomId>/join -H "X-User-Id: bob"
```

**FR-09 Studying now**: while Alice has an active focus session:

```bash
curl -i localhost:8084/friends/studying-now -H "X-User-Id: bob"
```

```bash
curl -i localhost:8084/rooms/<roomId>/studying-now -H "X-User-Id: bob"
```

**FR-07 Socratic tutor** (needs `LLM_API_KEY`): the full solution is only given after 2 real attempts. Ending the session makes the tutor ask a check question; your answer is graded 1–5 and sent to Planner (`UnderstandingRecorded`).

```bash
curl -i -X POST localhost:8082/tutoring-sessions -H "X-User-Id: alice" -H "Content-Type: application/json" -d '{"topicId":"<topicId>","question":"How do I write a recursive method that sums a list?"}'
```

```bash
curl -i -X POST localhost:8082/tutoring-sessions/<tutoringId>/messages -H "X-User-Id: alice" -H "Content-Type: application/json" -d '{"text":"sum(list) = first + sum(rest), and the empty list returns 0."}'
```

```bash
curl -i -X POST localhost:8082/tutoring-sessions/<tutoringId>/end -H "X-User-Id: alice"
```

```bash
curl -i localhost:8082/tutoring-sessions/<tutoringId> -H "X-User-Id: alice"
```

Errors use HTTP status codes: 400 invalid input, 404 not found (or another student's data), 409 conflict with the current state, 503 the LLM is unavailable.

## Tests
Domain rules have JUnit tests named after their requirement (`FR04_...`, `FR12_...`). The AI agents' tool rules are tested without calling the LLM.

```bash
mvn test
```

## Troubleshooting
- **Kafka connection errors in a service log:** start Kafka first and wait until `docker compose ps` shows it `healthy`, then start the services.
- **Progress or "studying now" stays empty after a Kafka upgrade or an old checkout:** reset Kafka with `docker compose down` and then `docker compose up -d`. This removes all Kafka data.
- **Tutor or plan returns 503:** the Gemini key is missing, wrong, or over its free-tier quota. Check `.env` and try again a minute later.

## Documentation
- [docs/01-domain.md](docs/01-domain.md): problem, scope, glossary
- [docs/02-requirements.md](docs/02-requirements.md): FR-01 to FR-12 with acceptance criteria
- [docs/03-bounded-contexts.md](docs/03-bounded-contexts.md): which service owns what
- [docs/05-events.md](docs/05-events.md): Kafka topics and event contracts
- [docs/09-conventions.md](docs/09-conventions.md): stack, packages, DDD rules
- [docs/ai-log/](docs/ai-log/): how AI was used
