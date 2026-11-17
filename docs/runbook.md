# Runbook

How to build, run, check and fix Furever Near. Written for someone who has never seen this project.
Steps marked **TODO (after first deploy)** get filled in once the API is live on Render.

- [Build and run from a fresh clone](#build-and-run-from-a-fresh-clone)
- [Run the tests](#run-the-tests)
- [Local problems and fixes](#local-problems-and-fixes)
- [Deploy](#deploy)
- [Roll back to the previous release](#roll-back-to-the-previous-release)
- [Rotate a secret](#rotate-a-secret)
- [Logs and health](#logs-and-health)
- [Database unreachable](#database-unreachable)

## Build and run from a fresh clone

### 0. Open a terminal and check what you need

Every command in this guide is typed into a terminal and run with Enter. Open one:

- **Windows:** press the Start key, type `PowerShell`, and open **Windows PowerShell**.
  (The terminal inside VS Code, `` Ctrl+` ``, works too.)
- **macOS:** press `Cmd+Space`, type `Terminal`, and press Enter.

Now check each tool below by typing its check command. If it prints a version, it is installed.
If it says *not recognized* or *command not found*, install it from the link, then **close and
reopen the terminal** before checking again.

| Tool | Needed for | Check it works | Get it |
|---|---|---|---|
| Git | getting the code | `git --version` | https://git-scm.com/downloads |
| Docker Desktop | running the API and database | `docker --version`, and the Docker Desktop window says *Engine running* | https://www.docker.com/products/docker-desktop/ (on Windows, keep **WSL 2** on. If Docker Desktop then says *WSL not installed*, see [Local problems and fixes](#local-problems-and-fixes)) |
| Node 22 or newer | the app and its tests | `node --version` shows `v22` or higher | https://nodejs.org/ (LTS) |
| JDK 21 | the API tests only | `java -version` shows `21`. It must be 21 exactly: older (`1.8`) and newer (`25`) do not work | https://adoptium.net/ (Temurin 21). In the Windows installer, turn on **Set JAVA_HOME variable** and **Add to PATH**. |

You do **not** need Postgres, Gradle, or any keys or `.env` file. Docker brings Postgres and Gradle with it.
To only run the stack (no tests), Git and Docker Desktop are enough.

### 1. Get the code

The terminal starts in your home folder. Any folder is fine: `git clone` makes a new
`PROJCET03` folder inside it. Then type:

```sh
git clone https://github.com/CST-438-P3/PROJCET03.git
cd PROJCET03
```

**Watch the spelling:** the repo is `PROJCET03` (C before E), not `PROJECT03`. Typing `PROJECT03`
gives `Repository not found`. Copy and paste the commands in this guide instead of retyping them.

All commands below start in this `PROJCET03` folder unless a step says `cd`.
If you close the terminal, open a new one and go back with `cd PROJCET03`.

### 2. Start the database and the API

Make sure Docker Desktop is open and running (its window says *Engine running*), then:

```sh
docker compose up --build
```

What you should see:

- The first time, Docker downloads Postgres and Java and builds the API. This takes **3 to 6 minutes**.
  Later runs take under a minute.
- The log ends with a line like `Started ApiApplication in 9.3 seconds`. The API is ready.
- A yellow `WARN ... SpringDoc /swagger-ui.html endpoint is enabled` line is normal, not an error.
- Newer Docker shows a menu at the bottom (`v View in Docker Desktop`, `d Detach`, ...). You can ignore it.
- This terminal is now busy showing logs. Leave it open. Open a **second terminal** for the next steps.

### 3. Check that the API is healthy

This only works while step 2 is still running. Wait until its terminal shows `Started ApiApplication`,
and keep that terminal open. If you closed it or pressed `Ctrl+C`, the API stopped; run step 2 again.

Open http://localhost:8080/actuator/health in a browser. You should see:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

Some browsers spread it over several lines (Chrome's *Pretty print*). That is the same thing.
`"status": "UP"` means the API and the database are both working.

From a terminal instead: `curl http://localhost:8080/actuator/health` (macOS/Linux) or
`curl.exe http://localhost:8080/actuator/health` (Windows PowerShell; plain `curl` there is a different command).

The API docs are at http://localhost:8080/swagger-ui.html.

### 4. Run the app (optional)

In the second terminal, with the API still running:

```sh
cd app
npm ci
npm run web
```

- `npm ci` takes 1 to 2 minutes the first time.
- When Expo says the web app is ready, open http://localhost:8081.
- Click **Sign in with GitHub** (sign-in is not wired to real GitHub yet, so it goes straight in), then **Search**.
  You should see pets named Luna and Max.
- Stop the app with `Ctrl+C`.

### 5. Stop everything

In the terminal running `docker compose up`, press `Ctrl+C`. Then:

```sh
docker compose down        # removes the containers, keeps the database
docker compose down -v     # also deletes the database, for a clean start
```

## Run the tests

You do not need `docker compose up` for the unit and API tests. Docker Desktop must be **running**,
because the API tests start their own Postgres with Testcontainers.

**API tests** (needs JDK 21):

```sh
cd api
./gradlew test          # macOS/Linux
.\gradlew.bat test      # Windows PowerShell
```

The first run downloads Gradle and takes a few minutes. Success ends with `BUILD SUCCESSFUL`.
The report is at `api/build/reports/tests/test/index.html`.

**App tests** (needs Node 22 or newer):

```sh
cd app
npm ci                  # skip if you already ran it
npm test
```

Success shows `Tests: N passed, N total` with no failures.

**End-to-end test** (optional; needs the API running from step 2):

```sh
cd app
npx playwright install chromium
npm run e2e
```

It builds the web app, opens it in a browser, signs in, searches, and checks accessibility with axe.
Success shows `1 passed`.

## Local problems and fixes

| What you see | What to do |
|---|---|
| `remote: Repository not found` on `git clone` | Check the spelling: `PROJCET03`, not `PROJECT03`. Copy the command from step 1. |
| `docker: command not found` or `'docker' is not recognized` | Not installed yet, or the terminal was opened before installing. Install Docker Desktop, open it once and wait for *Engine running*, then close and reopen the terminal. Still not found: restart the computer. |
| Docker Desktop says **WSL not installed** (Windows) | Open PowerShell **as administrator** (Start, type `PowerShell`, choose *Run as administrator*), run `wsl --install`, then restart the computer and open Docker Desktop again. |
| `Cannot connect to the Docker daemon` or `error during connect` | Docker Desktop is installed but not running. Open it and wait until it says it is running. |
| `port is already allocated` or `address already in use` for 8080 | Another program uses port 8080, often another project's API. Stop it, then run `docker compose up` again. |
| API exits with `Flyway` or `Schema-validation` errors | Your local database is from an older version. Run `docker compose down -v`, then `docker compose up --build`. |
| `./gradlew: Permission denied` (macOS/Linux) | Run `chmod +x gradlew` in the `api` folder, then try again. |
| `Gradle requires JVM 17 or later`, `Unsupported class file major version`, or Gradle cannot find Java 21 | Your terminal uses another Java version (often 1.8, sometimes 25). Install JDK 21 with **Set JAVA_HOME** and **Add to PATH** turned on and open a new terminal. Gradle uses `JAVA_HOME`: check it with `echo $env:JAVA_HOME` (Windows) or `echo $JAVA_HOME` (macOS); it should point to the JDK 21 folder. |
| API tests fail with `Could not find a valid Docker environment` | Testcontainers needs Docker. Open Docker Desktop and wait until it is running. |
| Windows: `npm.ps1 cannot be loaded because running scripts is disabled` (Korean Windows: `이 시스템에서 스크립트를 실행할 수 없으므로`) | A Windows PowerShell safety setting, not a project problem. Run `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` once and answer `Y`, then run the npm command again. Or type `npm.cmd` instead of `npm` (for example `npm.cmd ci`). |
| `npm ci` fails with an engine or syntax error | Your Node is too old. Install Node 22 or newer, open a new terminal, run `node --version`. |
| The browser says it can't reach `localhost:8080` | The API is not running. Look at the step 2 terminal: is it still open, and does it show `Started ApiApplication`? If it stopped with an error, find that error in this table. |
| The app shows "We couldn't load nearby pets" | The API is not running. Start it with step 2 and check step 3. |
| Sign-in with real GitHub or Google returns 401 | Expected without OAuth keys. Copy `.env.example` to `.env`, fill in `GITHUB_*` / `GOOGLE_*`, run `docker compose up` again. Never commit `.env`. |

If something else fails, open an issue with the step number, the exact command, the full output,
what you expected, and your OS and Docker version (`docker --version`).

## Deploy

The API runs on Render (free web service) from `render.yaml`, the database on Neon (free Postgres).
Production deploys only from a tagged GitHub release (`v0.3.0`, ...), cut by the sprint's merge captain.

1. The captain publishes the GitHub release for the sprint (tag `vX.Y.Z` on `main`).
2. In Render, open the `project03-api` service and deploy that tag's commit.
3. Wait for Render to show the deploy as live, then check health (see "Logs and health").

**TODO (after first deploy):** the API URL, the exact Render clicks, and the web app deploy.

## Roll back to the previous release

When a deploy breaks production, go back to the last release that worked:

1. In Render, open the `project03-api` service, then **Events** (or **Deploys**).
2. Find the deploy of the previous tag and choose **Rollback** (or redeploy that commit).
3. Check health. Then open an issue for the bad release and fix it on `main`.

Database migrations only go forward. If the bad release added a Flyway migration, the old code still
runs as long as the migration only added columns or tables; if it did more, ask the team before rolling back.

**TODO (after first deploy):** try a rollback once and correct these steps.

## Rotate a secret

Secrets live only in the Render environment and in each person's local `.env`, never in Git.

| Secret | Where to get a new one | What happens after the change |
|---|---|---|
| `JWT_SECRET` | Any random string of 32+ characters | Every user is signed out and signs in again. |
| `DATABASE_PASSWORD` | Neon: reset the role's password | Update Render first, then redeploy; the API is down until it restarts. |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth app settings: generate a new secret, delete the old one | GitHub sign-in fails until Render has the new value. |
| `GOOGLE_CLIENT_SECRET` | Google Cloud console: OAuth client, add a new secret, then delete the old one | Same as GitHub. |

Steps: make the new value, set it in Render **Environment**, let Render restart the service, check
health and sign in once, then delete the old value at the provider. If a secret was pushed to Git,
rotate it right away; deleting the commit is not enough.

## Logs and health

- Health: `GET /actuator/health`. Healthy is `"status":"UP"` with HTTP 200.
- Every request logs one line with an ID:
  `requestId=bc2a8e24-... method=GET path=/posts/nearby status=200 durationMs=52`
- The same ID comes back in the `X-Request-Id` response header. When someone reports an error,
  ask for that header (browser dev tools, Network tab) and search the logs for it.
- Local logs: the `docker compose up` terminal, or `docker compose logs api`.
- Production logs: Render, `project03-api` service, **Logs**. **TODO (after first deploy).**

## Database unreachable

Signs: health shows `"status":"DOWN"`, or requests fail with 500 and the logs show
`Connection refused` or `HikariPool` timeouts.

1. Local: is the `db` container running? `docker compose ps`. If not, `docker compose up`.
2. Production: open the Neon console. A free Neon project sleeps when idle; the first request
   wakes it, so wait a minute and check health again.
3. Check the Render environment values `DATABASE_HOST`, `DATABASE_NAME`, `DATABASE_USERNAME`,
   `DATABASE_PASSWORD`, and that `DATABASE_SSLMODE` is `require`.
4. If the password changed, follow "Rotate a secret" above.
5. If Neon itself is down, tell the team in Slack and post the Neon status page link. There is
   nothing to fix in our code.
