# 1. Run StudyAnchor from scratch (Windows)

This guide takes you from a fresh copy of the project to a running system you can demo with Postman. Follow the steps in order. Each step says what to type and what you should see.

**Time:** about 15 minutes the first time (installs), 3 minutes after that.

**What you get at the end:** Kafka running in Docker, the four services running in four windows, and Postman sending requests to them.

```
 Postman ──REST──►  planner-service :8081 ─┐
                    tutor-service   :8082 ─┤
                    focus-service   :8083 ─┼──►  Kafka :9092 (in Docker)
                    social-service  :8084 ─┘
```

---

## Step 1. Install the tools (once)

| Tool | Why | Get it |
|---|---|---|
| JDK 21 | Runs the Java services | https://adoptium.net (Temurin 21) |
| Maven 3.9+ | Builds the services | https://maven.apache.org/download.cgi (add its `bin` folder to `PATH`) |
| Docker Desktop | Runs Kafka | https://www.docker.com/products/docker-desktop |
| Postman | Sends requests to the services | https://www.postman.com/downloads |

Check them. Open **PowerShell** and run each line:

```powershell
java -version
```

```powershell
mvn -v
```

```powershell
docker -v
```

You should see `openjdk version "21...`, `Apache Maven 3.9...` and `Docker version ...`. If a command says *"is not recognized"*, that tool is not installed or not on `PATH`.

---

## Step 2. Open PowerShell in the project folder

1. Open the project folder (the one with `README.md` and `docker-compose.yml`) in File Explorer.
2. Click the address bar, type `powershell`, press **Enter**.

A PowerShell window opens already inside the project folder. Every command below runs from here.

---

## Step 3. Add the Gemini key (only for the two AI features)

The AI tutor (FR-07) and the AI weekly plan (FR-04) call Google Gemini. Everything else works without a key.

1. Get a free key at https://aistudio.google.com (→ *Get API key*).
2. Create the `.env` file:

```powershell
notepad .env
```

3. Notepad asks to create the file → **Yes**. Type this one line, with your key, then save and close:

```
LLM_API_KEY=paste-your-key-here
```

The file is git-ignored, so the key is never committed.

---

## Step 4. Start Docker Desktop and Kafka

1. Start **Docker Desktop** from the Start menu. Wait until it says *Engine running* (bottom left), about 30 seconds.
2. Start Kafka:

```powershell
docker compose up -d
```

Expected (last lines):

```
 Container studyanchor-kafka  Healthy
 Container studyanchor-kafka-init-1  Started
```

`kafka` is the message broker. `kafka-init` creates the six topics (`focus.sessions`, `planner.topics`, ...) and then exits. That is normal.

---

## Step 5. Build the four services

```powershell
mvn package
```

This compiles the code and runs all tests. Kafka must already be running (Step 4), because each service has one test that starts the whole app. It takes about 2 minutes (longer the first time, while Maven downloads libraries). Lots of log lines scroll past; that is normal. Expected at the end:

```
[INFO] Reactor Summary for StudyAnchor 0.1.0-SNAPSHOT:
[INFO]
[INFO] StudyAnchor ........................................ SUCCESS
[INFO] Planner Service .................................... SUCCESS
[INFO] Tutor Service ...................................... SUCCESS
[INFO] Focus Service ...................................... SUCCESS
[INFO] Social Service ..................................... SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

It creates one runnable file per service, for example `focus-service\target\focus-service-0.1.0-SNAPSHOT.jar`.

---

## Step 6. Start the four services (one window each)

Open **four** PowerShell windows in the project folder (repeat Step 2 four times). Paste one command into each:

Window 1:

```powershell
java -jar planner-service/target/planner-service-0.1.0-SNAPSHOT.jar
```

Window 2:

```powershell
java -jar tutor-service/target/tutor-service-0.1.0-SNAPSHOT.jar
```

Window 3:

```powershell
java -jar focus-service/target/focus-service-0.1.0-SNAPSHOT.jar
```

Window 4:

```powershell
java -jar social-service/target/social-service-0.1.0-SNAPSHOT.jar
```

Each window prints a lot of log lines. A service is ready when you see a line like:

```
Started FocusServiceApplication in 9.8 seconds
```

Leave all four windows open. Closing a window stops that service.

**Quick check in the browser.** Open http://localhost:8083/progress/new-student. You should see JSON like this (all zeros for a student who has never studied):

```json
{"userId":"new-student","today":{"date":"...","focus":"0m 0s","seconds":0},"last7Days":[...],"streakDays":0}
```

That answer comes from a Kafka Streams state store in the Focus service, so if you see it, Kafka and the services are talking.

---

## Step 7. Demo with Postman

### 7.1 Open the collection (once)
The collection comes in two formats with the same 97 requests:

| Format | Where | Use it for |
|---|---|---|
| **YAML** (Postman's local-files format) | `postman/collections/StudyAnchor/` (one file per request) + `.postman/resources.yaml` | **Newer Postman versions:** open the project folder as a local workspace |
| JSON (one file) | `postman/StudyAnchor.postman_collection.json` | Older Postman (**Import** → choose the file), and `newman` (7.3) |

**Newer Postman (YAML):**
1. Open Postman and choose to open a **local folder / local workspace**. Pick the **project folder** (the one with `README.md`), not the `postman` folder inside it.
2. The *StudyAnchor* collection appears with its 12 folders. Don't let Postman convert or move the JSON file: the YAML version is already there.

**Older Postman (JSON):** **Import** → choose `postman\StudyAnchor.postman_collection.json`.

**Either way:** **Settings (gear icon) → General → Working directory** → choose the project's `postman` folder. The material upload request needs this to find `sample-material.md`. (Or open that request's **Body** tab and pick the file by hand.)

### 7.2 Send requests
1. Open folder **0 Setup** → **New run: users and dates** → **Send**. This makes two fresh students (`alice-xxxx`, `bob-xxxx`) and today's dates. Every other request uses them automatically.
2. Go through the folders in order (1 FR-01, 2 FR-02, ...). For each request: click it, press **Send**, then look at:
   - the **response body** (the JSON the service returned), and
   - the **Test Results** tab (green = the service behaved as the requirement says).

Request names tell you what should happen, for example *"Same course code again → 409"* means the service must refuse a duplicate.

### 7.3 Run everything at once (optional)
Right-click the collection → **Run collection** → **Run StudyAnchor**. Expected: 97 requests, 0 failed. Folders 10 and 11 call Gemini, so a `503` there means the key is missing or the free quota is used up. Wait a minute and run that folder again.

### What to show while demoing
Put the **social-service** window next to Postman. When you send *Start session* (folder 5), a line appears there:

```
Presence: FocusSessionStarted for user alice-xxxx (session ..., topic null)
```

That is the Focus service publishing an event to Kafka and the Social service receiving it.

---

## Step 8. Stop everything

1. In each of the four service windows press **Ctrl+C**.
2. Stop Kafka:

```powershell
docker compose down
```

Courses, goals, sessions and the rest live in in-memory databases, so they are gone after a restart. (Progress totals are kept on disk by Kafka Streams, so an old student name may still show some minutes.) Run *0 Setup* again next time; it always makes new student names.

---

## If something goes wrong

| What you see | Why | Fix |
|---|---|---|
| `'mvn' is not recognized` | Maven is not on `PATH` | Add Maven's `bin` folder to `PATH`, open a new PowerShell |
| `error during connect ... dockerDesktopLinuxEngine` | Docker Desktop is not running | Start Docker Desktop, wait for *Engine running*, try again |
| `mvn package` fails with *"Failed to delete ... .jar"* | A service is still running and locks its jar | Press Ctrl+C in the service windows, then build again |
| `Port 8081 was already in use` | That service is already running in another window | Use the running one, or stop it with Ctrl+C |
| Kafka errors in a service window | Services started before Kafka was ready | Check `docker compose ps` shows `healthy`, then restart the service |
| Progress or "studying now" stays empty | Old Kafka data | `docker compose down`, then `docker compose up -d`, restart the services |
| Tutor or plan returns `503` | Gemini key missing, wrong, or over quota | Check `.env`, wait a minute, try again |
