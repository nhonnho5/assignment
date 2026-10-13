# AI log — 2026-10-13 — Project skeleton and FR-05 clarifications

**Tool:** Claude Code (desktop app)
**Phase:** Build step 1 (skeleton), grill session before Slice A (FR-05)
**Artefacts produced:** root `pom.xml`, four service modules, `docker-compose.yml`; edits to `02-requirements.md`, `09-conventions.md`, `progress.md`

## What the AI proposed and what I decided

| AI proposal | Decision | My rationale |
|---|---|---|
| Spring Boot 3.5.16 (latest 3.x) with Spring Cloud 2025.0.3, not Spring Boot 4.x | Accepted | *(write in your own words)* |
| Pin LangChain4j later (step 8), after checking its Spring starter supports Boot 3.5 | Accepted | *(write in your own words)* |
| No actuator; check a service is up by its default 404 JSON | Accepted | *(write in your own words)* |
| Kafka init container creates the six topics at startup (3 partitions each) | Accepted | *(write in your own words)* |
| FR-05: server computes actual minutes, rounded down | Modified — exact minutes and seconds instead; event field `actualMinutes` → `actualSeconds` | *(write in your own words)* |
| FR-05: ending twice → 409; ending another user's session → 404 | Accepted | *(write in your own words)* |
| FR-05: no automatic end at planned time | Accepted | *(write in your own words)* |
| FR-05: Focus stores time block / topic IDs without checking them | Accepted | *(write in your own words)* |
