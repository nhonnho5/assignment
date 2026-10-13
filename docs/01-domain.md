# 01 — Domain: StudyAnchor

> Status: draft v0.1 (2026-10-13)
> Origin: domain, problem and scope decided by the author; wording drafted with AI assistance (Claude) and reviewed by the author. See `ai-log/2026-10-13-domain-and-requirements.md`.
> "StudyAnchor" is a working name — rename freely.

## 1. Problem

University study demands strong executive function: deciding what to study, when, and for how long; starting and sustaining focus; and staying motivated over a whole semester. Students with ADHD (and many without) find this especially hard. Specific pain points:

- **Planning paralysis** — too many courses, topics and deadlines to turn into a realistic plan.
- **Distraction** — social media and phone use break focus sessions.
- **Passive AI use** — general-purpose AI chatbots give the answer immediately, so students skip the effort that produces learning.
- **Isolation** — studying alone makes it easy to stop; there is no one to be accountable to.
- **Losing the "why"** — long-term goals fade, and motivation drops mid-semester.

## 2. Stakeholders

| Stakeholder | Interest |
|---|---|
| Student (primary user) | A plan they can follow, sustained focus, real understanding, feeling less alone |
| Study friends / room members | Mutual accountability, seeing who is studying now |
| University (future) | Student retention and wellbeing; integration with the LMS (Moodle) |
| Accessibility / disability services (future) | Tools that support neurodivergent students |

## 3. Objectives

| ID | Objective |
|---|---|
| O1 | Reduce planning burden: the system proposes a realistic, time-blocked study plan. |
| O2 | Promote active learning: AI tutoring that requires student effort before giving answers. |
| O3 | Build sustained focus through session tracking and real-time feedback. |
| O4 | Reduce isolation through opt-in social accountability. |
| O5 | Keep motivation anchored to the student's own goals and values. |

## 4. Core capabilities

| Capability | Objectives | AI / real-time? |
|---|---|---|
| Course, topic and deadline management | O1 | — |
| AI-generated time-blocked study plans | O1 | Agentic AI |
| Study material management | O2 | — |
| Socratic AI tutor (hints first, answers last) | O2 | Agentic AI |
| Focus sessions and distraction logging | O3 | — |
| Real-time focus progress and streaks | O3 | Stream processing |
| Friends, study rooms, live "studying now" board | O4 | Stream processing |
| Personal goals, values and contextual reminders | O5 | Stream processing (trigger) |

## 5. Scope and simplifications (prototype)

- **No frontend.** All interaction is via REST APIs, demonstrated with a Postman collection.
- **No authentication.** The current user is identified by the `X-User-Id` request header.
- **Material ingestion** is by REST upload of a plain text, Markdown or PDF file; the text of a PDF is extracted on upload.
  *Rejected alternative:* scraping the university Moodle site — requires storing the student's university password, likely breaches IT policy, and breaks whenever the site changes. A production version would use Moodle's official Web Services API with a university-issued token.
- **Website blocking is out of scope** (it needs a browser extension or OS-level control). It is modelled instead as an *allowed-sites policy* on each focus session plus distraction logging, so a future extension can report off-policy site visits as events.
- **Reminders** are stored and retrieved via REST (no push notifications).
- **Presence sharing is opt-in** and private by default.

## 6. Ubiquitous language (glossary)

| Term | Meaning |
|---|---|
| Course | A university subject the student is enrolled in (e.g. CSCI318). |
| Topic | A unit of content within a course that can be studied. |
| Deadline | A dated assessment item linked to one or more topics, with a weight (%). |
| Availability | Weekly time slots the student is willing to study. |
| Time Block | A scheduled period to study a specific topic. |
| Study Plan | The set of time blocks generated for one week. |
| Goal | A personal objective with a "why" statement and optional target date. |
| Value Statement | A short personal idea or philosophy the student wants to be reminded of. |
| Reminder | A message to the student that references one of their goals or values. |
| Material | Study content (text) attached to a topic. |
| Tutoring Session | A conversation with the AI tutor about one question on one topic. |
| Attempt | A student's own try at answering during a tutoring session. |
| Understanding Level | A 1–5 rating of how well the student understands a topic. |
| Focus Session | A timed period of study, optionally linked to a time block. |
| Distraction | An interruption logged during a focus session. |
| Streak | Consecutive days with at least 25 focus minutes. |
| Friend | Another user who has accepted a friend request. |
| Study Room | A named group of users who study together. |
| Presence | Whether a user is currently in a focus session (visible only if sharing is on). |
