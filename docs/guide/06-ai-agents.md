# 6. The two AI agents

StudyAnchor has two LLM-based agents: the **Socratic tutor** (FR-07, tutor-service) and the **planning agent** (FR-04, planner-service). This part explains what makes them *agents*, how they are connected to Google Gemini with LangChain4j, and how the Java code keeps them inside the rules.

> Origin: written by Claude (AI) from the project's code, reviewed by the author. Code snippets are copied from the repo; `…` marks lines left out. Log lines are from a real run on 2026-10-19.

---

## 6.1 Chatbot vs agent

| A plain chatbot | An agent (what we built) |
|---|---|
| Text in → text out | Text in → the LLM **decides which tools to call** → our Java runs them → the LLM reads the results → it may call more tools → text out |
| Can only *say* things | Can *read* real data (deadlines, notes) and *change* the system (add time blocks, record an attempt) |
| Rules live only in the prompt, so the LLM may ignore them | Rules are **enforced in the tools and the domain**; the prompt only explains them |

The loop, step by step:

```
  student's message
        │
        ▼
  ┌──────────────┐  "call getTopics"    ┌────────────────────┐
  │  Gemini LLM  │ ───────────────────► │ Java tool method   │──► domain aggregate
  │              │ ◄─────────────────── │ (@Tool)            │    (checks the rules)
  └──────────────┘  result text / REFUSED: … └──────────────┘
        │   (repeat until the LLM has no more tool calls)
        ▼
  final reply to the student
```

LangChain4j runs this loop for us. We write the tools and the rules.

---

## 6.2 How an agent is wired with LangChain4j

Both agents are built the same way. From `tutor-service/…/infrastructure/GeminiTutorAgent.java`:

**1. Describe the agent as a Java interface.** LangChain4j generates the implementation:

```java
/** LangChain4j turns this interface into the agent: it sends the message, runs tool calls, returns the text. */
interface Tutor {
    String chat(String message);
}
```

**2. Create the Gemini model** (once, at start-up):

```java
// Gemini 3 signs each tool call ("thought signature") and rejects the next request unless it is sent back
this.model = apiKey.isBlank() ? null : GoogleAiGeminiChatModel.builder()
        .apiKey(apiKey).modelName(modelName).timeout(Duration.ofSeconds(60))
        .returnThinking(true).sendThinking(true).build();
```

- `apiKey` comes from `LLM_API_KEY` in `.env`. It is never in the code or in git.
- `modelName` defaults to `gemini-3.5-flash-lite` (setting `llm.model`, overridable with `LLM_MODEL`).
- No key → `model` is `null` → the service still starts, and only the AI endpoints answer **503**.

**3. Build the agent for this request:** model + memory + tools:

```java
ChatMemory memory = MessageWindowChatMemory.withMaxMessages(MEMORY_MESSAGES);
memory.add(SystemMessage.from(instructions(session, topic, topicMaterials)));        // the prompt
transcript.subList(0, transcript.size() - 1).forEach(m ->                           // earlier turns
        memory.add(m.fromStudent() ? UserMessage.from(m.text()) : AiMessage.from(m.text())));
Tutor tutor = AiServices.builder(Tutor.class)
        .chatModel(model)
        .chatMemory(memory)
        .tools(new TutoringTools(session, topicMaterials, solver, clock))           // tools bound to THIS session
        .toolArgumentsErrorHandler(ToolArgumentsErrorHandler.sendExceptionMessageToLlm())
        .toolExecutionErrorHandler(ToolExecutionErrorHandler.failInvocation())
        .build();
…
reply = tutor.chat(transcript.getLast().text());
```

- The **conversation is rebuilt from the database** every turn (the saved transcript), so nothing is lost if the service restarts.
- **Tools are created per request** and bound to one session or plan, so the LLM can only ever touch *this* student's data.
- If the LLM call fails, `TutorUnavailableException` → **503**, and `@Transactional` rolls back the student's message, so they can simply resend.

**4. A tool is a normal Java method with `@Tool`.** The description tells the LLM when to use it:

```java
@Tool("Returns the full worked solution to the student's question. It is refused until the student has made"
        + " 2 recorded attempts; when refused, give another hint instead.")
String revealSolution() { … }
```

---

## 6.3 Agent 1: the Socratic tutor (FR-07)

**Goal:** help the student *think*. Hints first; the answer only after real effort. This is objective O2 (active learning).

**Endpoints:** `POST /tutoring-sessions` (topic + question), `POST /tutoring-sessions/{id}/messages`, `POST /tutoring-sessions/{id}/end`, `GET /tutoring-sessions/{id}`.

**Its tools** (`tutor-service/…/infrastructure/TutoringTools.java`):

| Tool | What it does | Rule enforced in code |
|---|---|---|
| `searchMaterial(query)` | Finds the paragraphs of the student's own notes that best match | — (read only) |
| `recordAttempt()` | Marks the student's newest message as a real try | Only the newest student reply after a tutor question, and only once |
| `revealSolution()` | Asks a *separate* "Solver" LLM call for the worked solution | **Refused until 2 attempts** are recorded |
| `recordUnderstanding(level)` | Grades understanding 1–5 and ends the session | Only after `/end`, only after the student answered the check question, level 1–5 |

**The key guardrail lives in the domain aggregate**, not in the prompt: `tutor-service/…/domain/TutoringSession.java`

```java
/** AC3, AC8: the solution needs at least 2 recorded attempts. */
public void revealSolution() {
    requireStatus(SessionStatus.ACTIVE, "reveal the solution");
    if (attempts() < ATTEMPTS_FOR_SOLUTION) {
        throw new DomainException("The student has made " + attempts() + " attempt(s); "
                + ATTEMPTS_FOR_SOLUTION + " are needed before the solution");
    }
    solutionRevealed = true;
}
```

**How a broken rule reaches the LLM:** the tool turns the exception into a "REFUSED" message the LLM can read and react to, instead of crashing the request:

```java
private String refusedOr(String tool, Supplier<String> action) {
    String result;
    try {
        result = action.get();
    } catch (DomainException e) {
        result = "REFUSED: " + e.getMessage();
    }
    log.info("Tool {} in session {} → {}", tool, session.id(), result.lines().findFirst().orElse(""));
    return result;
}
```

**Why a separate "Solver"?** The tutor LLM never sees the full solution unless `revealSolution` succeeds. The solution is written by a different LLM call (`TutoringTools.Solver`) that can only be reached *through* that tool. So even a tutor LLM that wanted to "just give the answer" doesn't have it.

**Grounding in the student's notes.** `MaterialSearch` (a domain service) picks the 3 paragraphs closest to the question, and they are always put in the instructions. During testing the LLM sometimes skipped `searchMaterial`, so the code no longer relies on it.

**Session states:** `ACTIVE` (hints, attempts) → student presses end → `CHECKING` (the tutor asks one check question, then grades) → `ENDED`. Grading publishes `UnderstandingRecorded` → Planner uses the level to prioritise topics (part 05).

**Real tool log** (from the tool tests, no LLM involved):

```
Tool revealSolution in session 75d2… → REFUSED: The student has made 0 attempt(s); 2 are needed before the solution
Tool recordAttempt in session 75d2… → Attempt recorded. The student has made 1 attempt(s).
Tool revealSolution in session 75d2… → REFUSED: The student has made 1 attempt(s); 2 are needed before the solution
Tool recordUnderstanding(3) in session 7da2… → REFUSED: Cannot record understanding while the session is ACTIVE
Tool recordUnderstanding(7) in session 7da2… → REFUSED: Understanding level must be 1–5, was 7
```

---

## 6.4 Agent 2: the planning agent (FR-04)

**Goal:** turn deadlines + free time + understanding + focus history into a realistic weekly timetable (objective O1). **Endpoints:** `POST /plans` (`weekStart`, optional `replace`), `GET /plans/{weekStart}`.

**Its tools** (`planner-service/…/infrastructure/PlanningTools.java`):

| Tool | Reads / writes | Where the data comes from |
|---|---|---|
| `getDeadlines()` | read | Planner's `Course` aggregates |
| `getAvailability()` | read | `WeeklyAvailability`, turned into real dates for the week |
| `getTopics()` | read | Ranked by the `TopicPriority` domain service (part 04 §4.7) |
| `getFocusStats()` | read | **REST call** to Focus `GET /progress/{userId}` (the only service-to-service REST call) |
| `addBlocks(list)` | **write** | `StudyPlan.addBlock(…)`, which holds every block rule |
| `reportInsufficientTime(reason)` | **write** | `StudyPlan.reportShortfall(…)` |

**The write tool keeps valid blocks and sends back the reason for each invalid one**, so the LLM can correct itself:

```java
@Tool("Adds time blocks to the plan in one call. Valid blocks are kept; each invalid one comes back as"
        + " REFUSED with the reason, so fix and resend only those.")
String addBlocks(@P("the blocks to add") List<BlockProposal> blocks) {
    …
    for (int i = 0; i < blocks.size(); i++) {
        BlockProposal b = blocks.get(i);
        try {
            Ranked topic = topic(b.topicId());
            LocalDate date = parse(() -> LocalDate.parse(b.date()), "date must be yyyy-MM-dd");
            …
            plan.addBlock(topic.course(), topic.topic(), date, start, end, b.rationale(), availability,
                    clock.instant());
            added++;
        } catch (DomainException e) {
            refused.add("REFUSED block " + (i + 1) + " (" + b.date() + " " + b.start() + "–" + b.end() + "): "
                    + e.getMessage());
        }
    }
    …
}
```

The rules inside `StudyPlan.addBlock` (code, not prompt): inside one availability slot; 25–90 minutes; not in the past; at least 10 minutes from any other block; date inside the plan's week. `BlockProposal` uses plain strings, so a badly formatted date from the LLM becomes a polite "REFUSED", not a crash.

**Real tool log** from the run on 2026-10-19 (planner window):

```
Tool getDeadlines … → CSCI318 "Assignment 2" due 2026-11-02T23:59, weight 40 %, topics: Recursion, Domain-Driven Design
Tool getAvailability … → MONDAY 2026-10-26: 09:00–12:00
Tool getTopics … → 1. Domain-Driven Design (CSCI318), id 95d1…, understanding unknown, priority 8.1
Tool getFocusStats … → Today: 0m 11s. Last 7 days: … Streak: 0 day(s).
Tool addBlocks(5) … → Added 5 block(s); the plan now has 5 block(s).
```

The LLM chose to read all four sources, then added all its blocks in one call. Every one passed the code checks.

**Human in the loop.** If a plan already exists for that week, the request is refused (**409**) with a message telling the student to resend with `"replace": true`. The student decides; the agent never silently overwrites.

**After the plan:** time blocks are tracked by events (part 05). `FocusSessionStarted` / `Ended` with a `timeBlockId` → `STARTED` / `COMPLETED`; a block nobody started becomes `MISSED` (checked every minute by `MissedBlockScheduler`) and gets a reminder.

---

## 6.5 Why the rules are in code: three real cases

The spec asks for agentic AI; our project rule (in `CLAUDE.md`) adds: *"Agent business rules are enforced in tool code, not only in prompts."* Live testing showed why. All three cases are in [progress.md](../progress.md), steps 8 and 9:

| What the LLM did, despite the prompt | Fix in code |
|---|---|
| Graded the student straight after `/end`, without asking the check question | `TutoringSession.recordUnderstanding` refuses until the student has answered (test `FR07_understandingOnlyAfterStudentAnsweredCheckQuestion`) |
| Did not always call `searchMaterial` | The closest paragraphs are always included in the instructions |
| Said "not enough time" while free slots were still empty | `StudyPlan.reportShortfall` is refused while the plan is empty and a later slot is still free |

The lesson: **a prompt is a request; code is a guarantee.** This is good material for report §3.6 and §5.2.

---

## 6.6 Agentic design patterns used

| Pattern | Where |
|---|---|
| **Tool use / function calling** | Both agents: `@Tool` methods the LLM chooses to call |
| **Agent loop** (reason → act → observe → repeat) | LangChain4j `AiServices` runs it until the LLM stops calling tools |
| **Guardrails in code** | Domain aggregates (`TutoringSession`, `StudyPlan`) refuse rule-breaking tool calls |
| **Feedback for self-correction** | "REFUSED: <reason>" goes back to the LLM, which fixes and retries (`addBlocks`) |
| **Grounding (retrieval)** | `MaterialSearch` puts the student's own notes into the tutor's context |
| **Separate specialist call** | Tutor's `Solver` writes solutions; reachable only through `revealSolution` |
| **Human in the loop** | Replacing a plan needs the student's `"replace": true` |
| **Ports and adapters for the LLM** | Domain ports `TutorAgent` / `PlanningAgent`; Gemini adapters in `infrastructure`, so the LLM can be swapped |

---

## 6.7 Integration details

| Item | Value |
|---|---|
| Library | LangChain4j 1.22.0 (`AiServices`, `GoogleAiGeminiChatModel`, `@Tool`, `ChatMemory`) |
| Model | `gemini-3.5-flash-lite` by default (setting `llm.model`, env `LLM_MODEL`) |
| Key | `LLM_API_KEY` in `.env` (git-ignored), read via `${LLM_API_KEY:}` in `application.yml` |
| Timeouts | Tutor 60 s, planner 90 s |
| LLM down / no key / over quota | **503** with a clear message; the rest of the service keeps working |
| Gemini 3 detail | Each tool call carries a "thought signature" that must be sent back (`returnThinking(true).sendThinking(true)`); found during live testing |

---

## 6.8 Testing agents without calling the LLM

The rules are in the domain and the tools, so they are tested with plain JUnit, with no Gemini and no network:

| Test | File |
|---|---|
| `FR07_revealSolutionNeverCallsSolverBeforeTwoAttempts` | `tutor-service/src/test/…/infrastructure/TutoringToolsTest.java` |
| `FR07_revealSolutionCallsSolverAfterTwoAttempts` | same |
| `FR07_outOfRangeLevelIsRefused` | same |
| `FR04_invalidBlocksAreRefusedWithReasonAndValidOnesKept` | `planner-service/src/test/…/infrastructure/PlanningToolsTest.java` |
| `FR04_focusStatsUnavailableDoesNotStopPlanning` | same |
| `FR04_shortfallIsRecorded` | same |

The Solver is replaced by a fake that counts calls, so the first test proves the solution is **never even generated** before two attempts.

---

## 6.9 Try it yourself

Needs `LLM_API_KEY` in `.env`. In Postman:

- Folder **10 FR-07 Socratic tutor**: start a session, ask for the answer straight away (watch the tutor window log `REFUSED: … 0 attempt(s)`), make two tries, ask again, then end and answer the check question.
- Folder **11 FR-04 + FR-12 Weekly plan**: generate a plan and watch the planner window print the `Tool get… / addBlocks` lines; generate again without `replace` → 409.

A 503 means the key is missing or the free quota is used up. Wait a minute and resend.

---

**Next:** 07: Every feature (coming), one card per FR with endpoint, sample input and output, files and activity diagram.
