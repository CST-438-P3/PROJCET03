# ERD

Same model as the proposal slide, with column types. `users` and `identities` follow Appendix I,
section 3.5. Story IDs are from `docs/prd.md`. The schema is created by
`api/src/main/resources/db/migration/V1__init.sql`; change this file in the same PR as a migration.

```mermaid
erDiagram
  zip_codes ||--o{ users : "home ZIP"
  zip_codes ||--o{ shelters : "located in"
  users ||--o{ identities : "signs in with"
  users ||--o{ shelter_staff : "works at"
  shelters ||--o{ shelter_staff : "has staff"
  shelters ||--o{ adoption_posts : lists
  users ||--o{ favorites : saves
  adoption_posts ||--o{ favorites : "saved in"
  users ||--o{ inquiries : sends
  adoption_posts ||--o{ inquiries : "asked about"

  zip_codes {
    varchar zip PK
    double latitude
    double longitude
  }
  users {
    bigint id PK
    varchar email UK "unique ignoring case"
    varchar display_name "null if the provider gave no name"
    varchar role "USER or ADMIN"
    varchar zip_code FK "null until set (S3)"
    int radius_miles "default search distance (S3)"
    timestamptz created_at
  }
  identities {
    bigint id PK
    bigint user_id FK
    varchar provider "GITHUB or GOOGLE"
    varchar provider_user_id "Google sub or GitHub id, never the email"
    timestamptz created_at
  }
  shelters {
    bigint id PK
    varchar name
    varchar address
    varchar zip_code FK
    varchar phone
    varchar email
    timestamptz created_at
  }
  shelter_staff {
    bigint user_id PK, FK
    bigint shelter_id PK, FK
  }
  adoption_posts {
    bigint id PK
    bigint shelter_id FK
    varchar name
    varchar species "CAT or DOG"
    varchar breed
    varchar sex "MALE, FEMALE or UNKNOWN"
    varchar size "SMALL, MEDIUM or LARGE"
    int age_months
    varchar description
    varchar photo_url
    varchar status "AVAILABLE or ADOPTED"
    timestamptz created_at
    timestamptz updated_at
  }
  favorites {
    bigint user_id PK, FK
    bigint post_id PK, FK
    timestamptz created_at
  }
  inquiries {
    bigint id PK
    bigint user_id FK
    bigint post_id FK
    varchar message
    timestamptz created_at
  }
```

## Owners

| Table | Owner | Stories |
|---|---|---|
| `users`, `identities` | Role 1 (auth and accounts) | S1–S4 |
| `shelters`, `shelter_staff`, `adoption_posts` | Role 2 (shelters and posts) | S6, S7, S10 |
| `zip_codes`, `users.zip_code`, `users.radius_miles` | Role 3 (nearby search) | S3, S5 |
| `favorites`, `inquiries` | Role 4 (favorites and inquiries) | S8, S9 |

## Rules

- `identities` has a unique key on (`provider`, `provider_user_id`); `users.email` is unique ignoring case.
  Signing in with the other provider on an email that is already registered returns 409 (Appendix I, 3.5).
- A shelter needs a phone or an email (or both).
- A user is shelter staff for a shelter when a `shelter_staff` row exists. Only an admin adds or
  removes rows (S10).
- Staff can change only posts whose `shelter_id` is one of their shelters (S7).
- Nearby search returns only `AVAILABLE` posts, using the shelter's ZIP from `zip_codes` (S5, S7).
- Deleting a user deletes their `identities`, `shelter_staff`, `favorites` and `inquiries` rows (S4).
- `adoption_posts.updated_at` is set by a database trigger on every update.
- Inquiries are one-way (S9): staff see the sender's email and answer outside the app.
- Deleting a post deletes its `favorites` and `inquiries`.
