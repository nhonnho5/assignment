# AI log — 2026-10-14 — Step 10: Postman collection and README

**Tool:** Claude Code (desktop app)
**Phase:** Build step 10 (Postman collection + README), live FR-12 AC4 test carried over from step 9
**Artefacts produced:** `postman/StudyAnchor.postman_collection.json` and `postman/sample-material.md` (reviewed and extended), `README.md`, `.gitignore` edit

## What the AI proposed and what I decided

| AI proposal | Decision | My rationale |
|---|---|---|
| Verify the whole collection from the command line with newman (`npx`, a test runner only, not added to the project), instead of replaying it with curl | Accepted | *(write in your own words)* |
| Restore `.env.example` so the README can say "copy it to `.env`" | Rejected — I deleted it for good; the README explains how to create `.env` by hand (and the `!.env.example` line was removed from `.gitignore`) | *(write in your own words)* |
| Add requests for acceptance criteria the collection did not show: deleting a topic reaches Tutor (`TopicDeleted`, FR-01 AC9/10), deleting a goal and another student's delete → 404 (FR-11 AC6), accepting a friend request twice → 409 (FR-08 AC6), planning with no availability → 400 before the LLM is called (FR-04 AC9) | Accepted (part of the plan) | *(write in your own words)* |
| README with run steps, one curl sample per FR (each one run for real before writing it down), error codes and troubleshooting | Accepted (part of the plan) | *(write in your own words)* |

## Spec check and diagrams (same day, after step 10)

| AI proposal | Decision | My rationale |
|---|---|---|
| Spec check after step 10: list what the repo covers and what is missing; diagrams (use cases, activity per FR, domain model, architecture, layers, database) and SQL-like stream queries are drafted by AI with their origin labelled; the report, presentation and ai-log rationale stay the user's own work | Accepted | *(write in your own words)* |
| Draw the diagrams in PlantUML (real UML notation, PNG export for the report) rather than Mermaid (renders on GitHub, weaker UML) | Accepted | *(write in your own words)* |
| Render the PNGs through the public PlantUML server with a small script (`docs/diagrams/render.js`) instead of adding a local PlantUML/Graphviz install | Accepted (part of the PlantUML option) | *(write in your own words)* |
