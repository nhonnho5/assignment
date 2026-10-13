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
