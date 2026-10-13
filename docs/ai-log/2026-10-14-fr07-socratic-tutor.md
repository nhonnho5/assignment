# AI log — 2026-10-14 — FR-07 Socratic tutor agent

**Tool:** Claude Code (desktop app)
**Phase:** Grill session and plan for build step 8 (FR-07)
**Artefacts produced:** edits to `02-requirements.md` (FR-07 AC6–14), `05-events.md` (`UnderstandingRecorded` semantics), `09-conventions.md` (LangChain4j modules, model setting)

## What the AI proposed and what I decided

| AI proposal | Decision | My rationale |
|---|---|---|
| An attempt is recorded by the agent through a `recordAttempt` tool; code allows at most one per student message and none for the opening question (alternatives offered: every follow-up message counts; a separate attempts endpoint) | Accepted | *(write in your own words)* |
| The student ends the session with `POST /end`; the tutor asks a check question and grades the answer with a `recordUnderstanding(level)` tool (alternatives offered: the student rates themselves; the agent ends on its own) | Accepted | *(write in your own words)* |
| Grounding through a `searchMaterial(query)` tool that returns the 3 paragraphs sharing the most words (alternative offered: whole material in the prompt) | Accepted | *(write in your own words)* |
| Planner consumes `UnderstandingRecorded` in this step and shows `understandingLevel` per topic | Accepted | *(write in your own words)* |
| `revealSolution` asks a separate "solver" LLM call, reachable only through the 2-attempt check (alternative offered: the tool only grants permission and the tutor writes the solution) | Accepted | *(write in your own words)* |
| LLM failure → 503 and nothing saved; the service starts without a key | Accepted | *(write in your own words)* |
| Question and messages 1–2,000 characters; unknown/other student's → 404; message to an ended session or `/end` twice → 409 | Accepted | *(write in your own words)* |
| `TopicDeleted` also removes the topic's tutoring sessions | Accepted | *(write in your own words)* |
| Use LangChain4j 1.22.0 core + Gemini module without the (beta) Spring Boot starter; model `gemini-3.8-flash`, overridable with `LLM_MODEL` | Accepted (part of the plan) | *(write in your own words)* |

## Implementation (build step 8, same day)

| AI proposal | Decision | My rationale |
|---|---|---|
| Step 8 plan: `TutoringSession` aggregate holds every tool rule; tools only call it and turn a broken rule into "REFUSED: …" for the LLM; the solver is a separate LLM call behind `revealSolution` | Accepted | *(write in your own words)* |
| Mark attempts as a flag on the student's `Message` instead of a separate `Attempt` class (smaller; the transcript shows which messages were attempts) | Accepted (implementation detail, explained after the build) | *(write in your own words)* |
| `/end` adds the student line "I'd like to end the session now." to the transcript, so the conversation keeps alternating for Gemini and one agent method is enough | Accepted (implementation detail, explained after the build) | *(write in your own words)* |
| Curl test: Gemini 3 rejected the second request after a tool call (missing "thought signature"); fix with `returnThinking`/`sendThinking` | Accepted | *(write in your own words)* |
| Curl test: the LLM graded the student straight after `/end`, skipping the check question; enforce in the domain that grading needs the student's answer to the check question (new test first) | Accepted | *(write in your own words)* |
| Curl test: the LLM did not call `searchMaterial` on its first reply; always put the 3 closest material paragraphs in its instructions (AC4 guaranteed in code) | Accepted | *(write in your own words)* |
| Log each tool call at INFO as demo evidence of the agent's decisions | Accepted | *(write in your own words)* |
| Make `gemini-3.5-flash-lite` the default model, because `gemini-3.8-flash` was overloaded in the test and its free tier allows only 5 requests/minute (alternative offered: keep 3.8-flash and override in `.env`) | Accepted | *(write in your own words)* |
