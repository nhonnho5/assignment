# AI log — 2026-10-14 — FR-02 materials grill

**Tool:** Claude Code (cloud session)
**Phase:** Grill session for build step 6 (FR-02, `MaterialAdded` → Planner)
**Artefacts produced:** edits to `02-requirements.md` (FR-02 AC4–8), `09-conventions.md` (PDF library to ask the coordinator), `01-domain.md` (material ingestion)

## What the AI proposed and what I decided

| AI proposal | Decision | My rationale |
|---|---|---|
| Add material as JSON text (`{title, content}`) at `POST /topics/{topicId}/materials`; unknown or another student's topic → 404 | Modified — I want students to upload a file (I first asked for PDF). Material is now a multipart file upload at the same URL; 404 kept | *(write in your own words)* |
| PDF needs a PDF library that is not on the approved list; upload `.txt` / `.md` files now (no new dependency) | Accepted — build `.txt` / `.md` now, ask the coordinator about PDF and add it later only if approved | *(write in your own words)* |
| Title 1–100, content 1–50,000 characters, trimmed | Accepted | *(write in your own words)* |
| List + read materials, no edit or delete; `TopicDeleted` removes the topic's materials | Accepted | *(write in your own words)* |
| `GET /materials/{id}` returns JSON with the full text (not a raw file download) | Accepted | *(write in your own words)* |
| Planner keeps material IDs per topic from `MaterialAdded` and shows `materialCount` | Accepted | *(write in your own words)* |
