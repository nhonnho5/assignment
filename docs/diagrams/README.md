# Diagrams

> Origin: all diagrams were drafted with AI assistance (Claude Code) from the docs and the source code, then reviewed by the author. Each `.puml` file says the same in its first line.

The PlantUML sources (`.puml`) are the originals. The `png/` folder holds exported images for the report.

| Diagram | Source | Report section |
|---|---|---|
| Use cases (FR-01..FR-12) | [use-cases.puml](use-cases.puml) | §2.1 |
| Activity diagram per FR | [activity/](activity/) (`fr01-…` to `fr12-…`) | §2.1 |
| Domain model, grouped by sub-domain / bounded context | [domain-model.puml](domain-model.puml) | §2.4, §3.3 |
| Microservice architecture (REST, Kafka topics, data flows) | [architecture.puml](architecture.puml) | §3.1 |
| Layered architecture per service, mapped to packages | [layers.puml](layers.puml) | §3.2 |
| Database design (tables per service, state stores) | [database.puml](database.puml) | §3.4 |
| Stream processing queries (SQL-like) | [../06-stream-queries.md](../06-stream-queries.md) | §3.5 |
| Context map (upstream / downstream between contexts) | [context-map.puml](context-map.puml) | §2.4 |
| Sequence: one focus session feeds three consumers | [seq-focus-event-flow.puml](seq-focus-event-flow.puml) | §3.5 |
| Sequence: planning agent tool loop and guardrails | [seq-planning-agent.puml](seq-planning-agent.puml) | §3.6 |
| Sequence: Socratic tutor refusal, attempts, solver, grading | [seq-socratic-tutor.puml](seq-socratic-tutor.puml) | §3.6 |

## Re-render after editing
From the project root (needs Node.js 18+). The script sends each diagram's text to the public PlantUML server, so only use it for content you are happy to send there:

```bash
node docs/diagrams/render.js
```

Every line should start with `ok`. If one says `ERROR`, open its PNG: PlantUML draws the syntax error into the image.
