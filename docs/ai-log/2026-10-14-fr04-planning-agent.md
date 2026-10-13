# AI log — 2026-10-14 — FR-04 planning agent, FR-12 AC3/AC4 time blocks

**Tool:** Claude Code (desktop app)
**Phase:** Grill questions, plan and build step 9 (FR-04, FR-12 AC3/AC4)
**Artefacts produced:** edits to `02-requirements.md` (FR-04 AC8–15, FR-12 AC12), `03-bounded-contexts.md` and `05-events.md` (Planner consumes `FocusSessionStarted` too); planner-service code and tests

## What the AI proposed and what I decided

| AI proposal | Decision | My rationale |
|---|---|---|
| A second plan request for the same week replaces the old plan (alternative offered: 409 + a DELETE endpoint) | Modified — I asked that the student is reminded first and then given the choice: 409 with a message, and `"replace": true` to confirm | *(write in your own words)* |
| A block is "missed" only if no focus session was started for it by its end; Planner also reads `FocusSessionStarted` (alternative offered: missed if not completed by its end) | Accepted | *(write in your own words)* |
| "Not enough time" is reported by the agent through a `reportInsufficientTime(reason)` tool into a `shortfall` field (alternative offered: a fixed code rule about deadlines in the next 14 days) | Accepted | *(write in your own words)* |
| Topic priority is computed in code — (6 − understanding level) × Σ(weight ÷ days until due) — and `getTopics` returns that order (alternative offered: prompt only) | Accepted | *(write in your own words)* |

## Implementation (build step 9, same day)

| AI proposal | Decision | My rationale |
|---|---|---|
| Step 9 plan: `StudyPlan` aggregate checks every block rule; `TimeBlock` entity with status PLANNED/STARTED/COMPLETED/MISSED; `TopicPriority` domain service; tools call the aggregate and turn a broken rule into "REFUSED: …" | Accepted | *(write in your own words)* |
| Defaults stated in the plan: a block fits inside one slot (touching slots are not joined), starts after now, rationale 1–200 characters; no availability or no topics → 400 before the LLM is called; LLM failure → 503; Focus down → the tool says "unavailable" and planning continues | Accepted (part of the plan) | *(write in your own words)* |
| One `addBlocks(list)` tool instead of one call per block, to keep the number of LLM requests low on the free tier | Accepted (part of the plan) | *(write in your own words)* |
| Read Focus statistics with Spring's `RestClient` (part of Spring Web, no new dependency) | Accepted (part of the plan) | *(write in your own words)* |
| Curl test: the LLM reported "not enough time" and left a free slot empty, even after a clearer prompt; enforce in the domain that the shortfall is refused while the plan is empty and a later slot is still free (new test first) | Accepted (found in testing and fixed before review) | *(write in your own words)* |
| Finish the live missed-block test (wait ~30 min for a real block to end) before ticking step 9 | Modified — I closed step 9 with that live test moved to the start of step 10 (it is covered by unit tests; a Planner restart had reset the first attempt) | *(write in your own words)* |
