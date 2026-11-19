# PROJECT03

<!-- One line: what it is and who it's for. See docs/prd.md. -->

## Layout

| Path | What |
|---|---|
| `api/` | Spring Boot (Gradle, Java 21) OAuth2 Resource Server |
| `app/` | Expo (React Native, TypeScript), web + Android |
| `docs/` | PRD, architecture, ADRs, security, runbook, retros |

## Run locally

See [`docs/runbook.md`](docs/runbook.md#build-and-run-from-a-fresh-clone) for prerequisites and the full local setup.

```sh
cp .env.example .env            # optional; fill in values, never commit
docker compose up --build       # http://localhost:8080
```

The app keeps the key on the backend. Searches use the Rescue Groups available
animals endpoint and show at most 10 results per page.

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
