# Runbook

## Build and run from a fresh clone

Prerequisites are Git, Docker Desktop, Node 22+, and JDK 21. Postgres and Gradle are provided by Docker.

```sh
git clone https://github.com/CST-438-P3/PROJCET03.git
cd PROJCET03
cp .env.example .env
docker compose up --build
```

The API is ready at `http://localhost:8080/actuator/health` when it reports `{"status":"UP"}`.
Run the Expo app in a second terminal:

```sh
cd app
npm ci
npm run web
```

To use live Rescue Groups data, put the key in `.env` and enable the profile before starting Compose:

```sh
SPRING_PROFILES_ACTIVE=pet-provider RESCUE_GROUPS_API_KEY=<your-key> docker compose up --build
```

On Windows PowerShell, set the variables first:

```powershell
$env:SPRING_PROFILES_ACTIVE = "pet-provider"
$env:RESCUE_GROUPS_API_KEY = "<your-key>"
docker compose up --build
```

Never commit `.env` or an API key. The key is passed only to the backend; Expo never receives it.

## Run the tests

```sh
cd api && ./gradlew test
cd ../app && npm test
```

API tests require Docker because they use Testcontainers.

## Stop and reset

Stop the foreground stack with `Ctrl+C`, then run `docker compose down`. To remove the local database too, use
`docker compose down -v` (this deletes local data).

## Deploy, rollback, secrets, and health

Production deployment uses `render.yaml` with Neon Postgres. Check `/actuator/health` after deployment and inspect
Render logs for failures. Rotate `JWT_SECRET`, database credentials, OAuth secrets, and `RESCUE_GROUPS_API_KEY`
only in the deployment environment; never store real values in Git.

## Database unreachable

Run `docker compose ps` and confirm the `db` health check is passing. If the local schema is stale, stop the stack
and run `docker compose down -v`, followed by `docker compose up --build`.
