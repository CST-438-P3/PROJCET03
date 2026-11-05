# PROJECT03

<!-- One line: what it is and who it's for. See docs/prd.md. -->

## Layout

| Path | What |
|---|---|
| `api/` | Spring Boot (Gradle, Java 21) OAuth2 Resource Server |
| `app/` | Expo (React Native, TypeScript), web + Android |
| `docs/` | PRD, architecture, ADRs, security, runbook, retros |

## Run locally

Prerequisites: JDK 21, Node 22, a Postgres on `localhost:5432` (db/user/password `app`).

```sh
cp .env.example .env            # fill in values, never commit
cd api && JWT_SECRET=<32+ chars> ./gradlew bootRun   # http://localhost:8080
cd app && npm install && npm run web                  # http://localhost:8081
```

API contract: `api/src/main/resources/static/openapi.yaml`, Swagger UI at `/swagger-ui.html`.

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

```sh
cd api && ./gradlew test        # needs Docker (Testcontainers)
cd app && npm test              # Jest
cd app && npm run e2e           # Playwright + axe
```

## Live URLs

- API: TBD
- Web: TBD
