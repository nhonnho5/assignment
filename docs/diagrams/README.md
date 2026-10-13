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

## Re-render after editing
From the project root (needs Node.js 18+). The script sends each diagram's text to the public PlantUML server, so only use it for content you are happy to send there:

```bash
node docs/diagrams/render.js
```

Every line should start with `ok`. If one says `ERROR`, open its PNG: PlantUML draws the syntax error into the image.
