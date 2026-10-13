---
name: slice
description: Implement the next build-order step (or a named FR) as one vertical slice with TDD, ending in a curl test and a commit. Use when the user says "/slice", "next slice", or "implement FR-xx".
argument-hint: "[FR-xx | next]"
---

# Implement one slice

Argument: `$ARGUMENTS` (an FR ID, or `next` / empty = the first unchecked item in `docs/progress.md`).

## 1. Orient
- Read `docs/progress.md` and the docs listed in `CLAUDE.md`. Do not jump ahead of the build order.
- If any acceptance criterion is ambiguous, stop and run the `grill-me` process for it first.

## 2. Plan (wait for approval)
Present a short plan: the classes to create, grouped by layer (`domain`, `service`, `infrastructure`, `presentation`), the DDD pattern of each, the events published/consumed, and the test list (named `FRxx_rule`). Wait for the user to approve.

## 3. Build, inside-out, test first
1. **Domain** — write a failing JUnit test for one invariant, make it pass, repeat. Pure Java, no Spring.
2. **Service** — application service: load, call aggregate, save, then publish registered events via `EventPublisher`.
3. **Infrastructure** — JPA adapters, `StreamBridge` publisher, Kafka consumers/topologies, LangChain4j tools (tool rules enforced in code and unit-tested without the LLM).
4. **Presentation** — controller + DTOs, `X-User-Id` header, `DomainException` → HTTP status via the exception handler.

Run `mvn -q -pl <module> test` after each layer.

## 4. Verify for real
- `docker compose up -d`, start the affected services, and run the curl commands yourself. For events, show the consumer's log line.
- Give the user the exact curl commands to repeat it.

## 5. Close out
- Explain each new class in plain language: its layer, why it's there, which DDD pattern it uses.
- Tick the item in `docs/progress.md`.
- Commit: `FR-xx: <summary>` (only after the user confirms they're happy).
- If the user accepted/changed/rejected a proposal, run the `ai-log` skill.
