# AI log — 2026-10-13 — Tooling setup and requirement clarifications

**Tool:** Claude Code (desktop app)
**Phase:** Project setup, requirements clarification
**Artefacts produced:** `.claude/` (settings, compile-check hook, skills `grill-me`, `slice`, `ai-log`), `.gitignore`, `.gitattributes`, `.env.example`, `docs/progress.md`; edits to `CLAUDE.md`, `02-requirements.md`, `09-conventions.md`

## What the AI proposed and what I decided

| AI proposal | Decision | My rationale |
|---|---|---|
| Workflow: git from day one, test-first domain rules, one slice per session, compile check after every edit | Accepted | *(write in your own words)* |
| Google Gemini as LLM provider (free tier) | Accepted | *(write in your own words)* |
| A "day" for streaks is a calendar day in `Australia/Sydney`, not UTC | Accepted | *(write in your own words)* |
| `DomainException` → 400, `NotFoundException` → 404, `ConflictException` → 409 | Accepted | *(write in your own words)* |
| New FR-12 criterion: `FocusSessionEnded` with a `timeBlockId` marks the block completed | Accepted | *(write in your own words)* |
| FR-09 sharing filter applied at query time, not inside the stream topology | Accepted | *(write in your own words)* |
| `@Scheduled` job every minute detects missed time blocks | Accepted | *(write in your own words)* |
| FR-04 week start date must be a Monday | Accepted | *(write in your own words)* |
