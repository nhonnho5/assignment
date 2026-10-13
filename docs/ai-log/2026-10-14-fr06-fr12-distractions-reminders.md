# AI log — 2026-10-14 — FR-06, FR-12 distractions, spikes and reminders

**Tool:** Claude Code (desktop app)
**Phase:** Grill session and plan for build step 7 (FR-06, FR-12)
**Artefacts produced:** edits to `02-requirements.md` (FR-06 AC5–8, FR-12 AC8–11), `05-events.md` (`DistractionSpikeDetected` semantics)

## What the AI proposed and what I decided

| AI proposal | Decision | My rationale |
|---|---|---|
| Move FR-12 AC3/AC4 (time block completed, missed-block reminders) to step 9, because time blocks only exist once the planning agent (FR-04) creates them | Accepted | *(write in your own words)* |
| Skip FR-12 AC5 (random reminders, optional) and AC7 (LLM-phrased text, stretch) for now; revisit after step 10 | Accepted | *(write in your own words)* |
| Fixed (tumbling) 30-minute windows aligned to the clock; alert once when a window reaches its 3rd distraction; boundary-straddling distractions are not combined | Accepted (alternative offered: sliding windows, more accurate but needs de-duplication) | *(write in your own words)* |
| Count distractions per student across all sessions, not per session | Accepted | *(write in your own words)* |
| Compare sites as hosts (strip scheme, `www.`, port, path); a subdomain of an allowed site is allowed | Accepted | *(write in your own words)* |
| Reminder quotes a random not-yet-achieved goal or value statement; with nothing to quote, a general message plus a tip to add a goal | Accepted | *(write in your own words)* |
| Marking a reminder read twice → 409 (same as achieving a goal twice) | Accepted | *(write in your own words)* |
| Smaller defaults: `POST /sessions/{id}/distractions`; ended → 409, other student's → 404; type case-insensitive; site ≤ 200, note ≤ 500; session shows its distractions; note not in the event; repeated spike ignored; `GET /reminders` unread newest first; `POST /reminders/{id}/read` | Accepted | *(write in your own words)* |
| Set each Focus Kafka record's timestamp to the event's `occurredAt`, so the spike window uses the logging time | Accepted (part of the plan) | *(write in your own words)* |
| Step 7 plan: `Distraction` as a value object inside `FocusSession`, `Site` value object, spike rule held by the `DistractionSpikeDetected` event, `Reminder` aggregate with a `triggerKey` for de-duplication | Accepted | *(write in your own words)* |

## Implementation (build step 7, same day)

| AI proposal | Decision | My rationale |
|---|---|---|
| Spike count with caching turned off and a 1-minute grace period, each Kafka Streams function with its own `applicationId` | Accepted (implementation detail, explained after the build) | *(write in your own words)* |
| Curl verification found an FR-10 bug: Kafka 4 rejects the progress store's changelog records (stamped up to 11 h ahead on the Sydney timeline), so the stream died ~30 s after a session ended. Fix in the code: the changelog topic allows timestamps up to 1 day ahead (alternative offered: change the broker setting in `docker-compose.yml`) | Accepted — fix in the code | *(write in your own words)* |
| Commit the FR-10 fix separately from step 7 | Accepted | *(write in your own words)* |
