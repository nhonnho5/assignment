# 09 — Conventions

> Status: draft v0.1 (2026-10-13). Constraints come from the CSCI318 project specification; details drafted with AI assistance.

## Tech stack (mandated by the subject)
- JDK 21, Spring Boot 3.x, Maven.
- Spring Web, Spring Data JPA, H2 (in-memory), Spring Cloud Stream (Kafka binder), LangChain4j.
- Apache Kafka (single broker, KRaft mode, via `docker-compose.yml`).
- Use the latest stable versions that are mutually compatible (Spring Boot ↔ Spring Cloud release train ↔ LangChain4j Spring Boot starter). Pin them once in the root `pom.xml`.

## Dependency rule
Only the dependencies above, plus `spring-boot-starter-test`. **Any other dependency needs coordinator approval** — do not add it.

| Dependency | Status |
|---|---|
| `spring-cloud-stream-binder-kafka-streams` | ✓ Approved by coordinator *(2026-10-13)* — used for FR-09, FR-10, FR-12 |
| Lombok | ✗ Not used — Java records replace it |
| `spring-boot-starter-validation` | ✗ Not used — invariants are enforced in domain code |
| `langchain4j-google-ai-gemini` | ✓ LLM provider module, part of LangChain4j *(decided 2026-10-13)* |

## Repository layout
```
studyanchor/
  pom.xml                (parent, versions pinned)
  docker-compose.yml     (Kafka)
  CLAUDE.md
  README.md
  docs/
  postman/
  planner-service/
  tutor-service/
  focus-service/
  social-service/
```
Services do **not** share code modules or databases.

## Package structure (every service)
```
com.studyanchor.<service>
  presentation     REST controllers, request/response DTOs, exception handler
  service          application services: use-case orchestration, transactions, publishing events
  domain           aggregates, entities, value objects, domain events, domain services,
                   repository interfaces (plain Java)
  infrastructure   Spring Data repositories + adapters, Kafka publishers/consumers,
                   Kafka Streams topologies, LangChain4j config and tools, REST clients
```
Dependencies point inward: `presentation → service → domain ← infrastructure`.

## DDD rules
- **Aggregates** are changed only through methods on the aggregate root. No public setters. Invariants are checked in constructors and methods, and throw `DomainException`.
- **Value objects** are Java `record`s, immutable and validated in the compact constructor; persisted with `@Embeddable`.
- **Domain events** are records in the `domain` package. Aggregates register events; the application service publishes them *after* saving, through an `EventPublisher` interface implemented in `infrastructure` with `StreamBridge`.
- **Repository interfaces** live in `domain`; Spring Data interfaces and adapters live in `infrastructure`.
- *Pragmatic choice:* JPA annotations are allowed on domain classes to keep the code small. (Trade-off to discuss in the report.)
- **Controllers contain no business logic.**
- IDs are `UUID`. The current user comes from the `X-User-Id` header.
- **Errors** *(decided 2026-10-13)*: `DomainException` → 400; its subclass `NotFoundException` → 404; its subclass `ConflictException` → 409. Each service has one `@RestControllerAdvice` in `presentation` that does this mapping.
- **Time** *(decided 2026-10-13)*: instants are stored in UTC. Anything that depends on "which day" (streaks, daily minutes, week starts) uses the zone `Australia/Sydney`, defined once as a constant per service.
- **Scheduling** *(decided 2026-10-13)*: Spring `@Scheduled` (part of Spring, no extra dependency) is used for time-triggered jobs, e.g. FR-12 missed blocks.

## Kafka
- Spring Cloud Stream functional style: `Consumer<...>` / `Function<...>` beans for consuming; `StreamBridge` for publishing.
- Topics and keys exactly as in `05-events.md`.
- Bootstrap server: `localhost:9092`.

## LLM
- API key from environment variable `LLM_API_KEY`; never commit keys.
- Business rules the agent must respect (e.g. time-block limits, answer-reveal rule) are **enforced in code inside the tools**, not only in prompts.

## Traceability
- Each class that implements a requirement lists it in its Javadoc, e.g. `/** Implements FR-05. */`.
- Commit messages start with the FR ID, e.g. `FR-05: start and end focus sessions`.
- Domain unit tests are named after the FR and rule, e.g. `FR05_cannotStartSecondActiveSession`.

## Open decisions (author to decide)
- [x] **LLM provider:** Google Gemini (free tier) via LangChain4j; key in `LLM_API_KEY`. *(decided 2026-10-13)*
- [x] Confirm Kafka Streams binder with the coordinator. *(approved 2026-10-13)*
- [ ] Final application name.
