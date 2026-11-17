# PROJECT03

Furever Near: find adoptable cats and dogs at shelters near you. A Spring Boot API with an Expo app for web and Android. See `docs/prd.md`.

## Layout

| Path | What |
|---|---|
| `api/` | Spring Boot (Gradle, Java 21) OAuth2 Resource Server |
| `app/` | Expo (React Native, TypeScript), web + Android |
| `docs/` | PRD, architecture, ADRs, security, runbook, retros |

## Run locally

**Full step-by-step guide, with expected output and fixes: [`docs/runbook.md`](docs/runbook.md#build-and-run-from-a-fresh-clone).**

Short version. You need Git and Docker Desktop (running); Node 22 for the app; JDK 21 for the API tests.
No `.env` or keys are needed to run locally.

```sh
git clone https://github.com/CST-438-P3/PROJCET03.git
cd PROJCET03
docker compose up --build        # first time 3-6 min; ready at "Started ApiApplication"
```

Then open http://localhost:8080/actuator/health and look for `"status":"UP"`.
Swagger UI: http://localhost:8080/swagger-ui.html

App (optional, second terminal): `cd app && npm ci && npm run web`, then open http://localhost:8081.

## Deploy API to Render with Neon Postgres

The root `render.yaml` deploys the Spring API on Render and prompts for
Neon connection settings; the database itself is hosted on Neon. Create a Neon
project, then create a Blueprint in Render from this repository. When prompted,
copy the host, database name, username, and password from Neon into the
corresponding fields, and set `DATABASE_SSLMODE` to `require`. Keep the database
password secret. Render generates `JWT_SECRET`.

The Expo app is not deployed by this Blueprint; set `EXPO_PUBLIC_API_URL` in its
build environment to the deployed API URL when configuring an app deployment.
For a web deployment, also set the API's `CORS_ORIGINS` to the web app's exact
origin.

## Tests

Docker Desktop must be running. See the runbook for expected output.

```sh
cd api && ./gradlew test        # JDK 21; Windows PowerShell: .\gradlew.bat test
cd app && npm ci && npm test    # Node 22
cd app && npm run e2e           # optional; needs the API from docker compose up
```
