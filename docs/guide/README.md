# StudyAnchor guide

A step-by-step guide to running and understanding StudyAnchor, written for someone who has never seen the code. Read the parts in order, or jump to the one you need.

> Origin: written by Claude (AI) from the project's docs and code, reviewed by the author. Sample outputs are from real runs on 2026-10-19.

| Part | What you learn | Read it before writing report section |
|---|---|---|
| [01 Run it](01-run-it.md) | Install, build, start, demo with Postman, stop, fix common problems | §4.1 |
| [02 Big picture](02-big-picture.md) | The problem, the 12 features, the 4 services, how they talk, one student's day | §1, §3.1 |
| [03 Inside one service](03-inside-a-service.md) | The 4 layers, and one request (FR-05) traced through every file | §3.2 |
| [04 DDD patterns](04-ddd-patterns.md) | Entity, value object, aggregate, event, domain service, repository, with every domain class mapped | §2.4, §3.3 |
| [05 Kafka and streams](05-kafka-and-streams.md) | Publishing, consuming, and the 3 real-time streams (SQL next to Java) | §3.4, §3.5 |
| [06 AI agents](06-ai-agents.md) | The tutor and planning agents, their tools, and the rules enforced in code | §2.3, §3.6 |
| [07 Every feature](07-every-feature.md) | One card per FR: endpoints, real input and output, files, activity diagram | §2.1, §4.2 |

Diagrams are in [`docs/diagrams/png/`](../diagrams/png/). The report checklist is [`docs/report-outline.md`](../report-outline.md).
