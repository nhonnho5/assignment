# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`docs/01-domain.md` §6 "Ubiquitous language (glossary)"**: this repo's glossary. It plays the role of `GLOSSARY.md`; do not create a separate `GLOSSARY.md`.
- **`docs/03-bounded-contexts.md`**: which service (Planner, Tutor, Focus, Social) owns what.
- **`docs/05-events.md`**: Kafka topics and event contracts (exact names and fields).
- **`docs/adr/`**: read ADRs that touch the area you're about to work in.

If `docs/adr/` doesn't exist, **proceed silently**. Don't flag its absence; don't suggest creating it upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates it lazily when decisions actually get resolved. New or changed terms go into `docs/01-domain.md` §6.

If code and docs disagree, the docs win (see `CLAUDE.md`).

## File structure

Single-context repo:

```
/
├── docs/
│   ├── 01-domain.md          ← glossary in §6
│   └── adr/
│       ├── 0001-<decision>.md
│       └── 0002-<decision>.md
├── planner-service/
├── tutor-service/
├── focus-service/
└── social-service/
```

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `docs/01-domain.md` §6. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal: either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (event-sourced orders), but worth reopening because…_
