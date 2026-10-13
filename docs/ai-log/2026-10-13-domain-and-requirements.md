# AI log — 2026-10-13 — Domain and requirements

**Tool:** Claude (claude.ai chat)
**Phase:** Requirements engineering, domain modelling
**Artefacts produced:** `docs/01-domain.md`, `02-requirements.md`, `03-bounded-contexts.md`, `05-events.md`, `09-conventions.md`, `CLAUDE.md`

## What I brought (author's input)
Four needs from my own experience as a student with ADHD:
1. Help with time blocking, scheduling and choosing topics; automatic import of material from the uni website; an app that only lets me use the uni site.
2. AI help that makes me learn instead of skipping to the answer.
3. A focus trainer that logs time, shows progress, and shows friends who is studying (like Korean study apps).
4. Reminders tied to my personal goals and philosophy.

## What the AI proposed and what I decided

| AI proposal | Decision | My rationale |
|---|---|---|
| Four bounded contexts: Planner, Tutor, Focus, Social | Accepted | *(write in your own words)* |
| Keep goals/reminders inside Planner rather than a 5th service | Accepted | *(write in your own words)* |
| Replace Moodle scraping with a material-upload endpoint; justify Moodle Web Services API as the production approach | Modified my original idea | *(e.g. storing my uni password is a security risk...)* |
| Model website blocking as an allowed-sites policy + distraction logging instead of building a blocker | Modified my original idea | *(write in your own words)* |
| Socratic tutor with a code-enforced "≥ 2 attempts before solution" rule | Accepted | *(why enforcing in code matters vs prompt only)* |
| Message key = userId on all events | Accepted | *(ordering per user)* |

## Notes for the report (Section 5)
- *(What surprised you, what you had to correct, what you'd do differently.)*
