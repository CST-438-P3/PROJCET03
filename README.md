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

## Tests

```sh
cd api && ./gradlew test        # needs Docker (Testcontainers)
cd app && npm test              # Jest
cd app && npm run e2e           # Playwright + axe
```

## Live URLs

- API: TBD
- Web: TBD
