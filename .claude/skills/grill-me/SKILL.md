---
name: grill-me
description: Interview the user about a requirement, slice or design decision until every ambiguity is resolved, then write the answers back into docs/. Use before starting a slice, or when the user says "grill me".
---

# Grill me

Goal: no ambiguity survives into code. The docs are the spec; fix the spec first.

1. Read the relevant FRs in `docs/02-requirements.md` plus `03-bounded-contexts.md`, `05-events.md` and `09-conventions.md`.
2. List every question where two reasonable developers could implement it differently: time zones, boundaries (inclusive/exclusive), HTTP status codes, nullability, ownership between services, ordering of events, what happens on duplicates or missing data.
3. Ask the user **one question at a time** (or a small batch with AskUserQuestion), each with your recommended answer and a one-line reason. The user is new to Java — explain trade-offs in plain language.
4. Keep going until the user says stop or nothing material is left.
5. Write each decision into the right doc (acceptance criteria, conventions, or events). Mark it `(decided YYYY-MM-DD)`.
6. Add a row to today's `docs/ai-log/` file for each AI proposal the user accepted, modified or rejected — leave the rationale column for the user.
