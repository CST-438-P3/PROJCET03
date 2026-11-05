# ERD

Same model as the proposal slide, with column types. Story IDs are from `docs/prd.md`.
Change this file through a PR before changing a migration.

```mermaid
erDiagram
  zip_codes ||--o{ users : "home ZIP"
  zip_codes ||--o{ shelters : "located in"
  users ||--o{ shelter_staff : "works at"
  shelters ||--o{ shelter_staff : "has staff"
  shelters ||--o{ adoption_posts : lists
  users ||--o{ favorites : saves
  adoption_posts ||--o{ favorites : "saved in"
  users ||--o{ inquiries : sends
  adoption_posts ||--o{ inquiries : "asked about"

  zip_codes {
    char5 zip PK
    double latitude
    double longitude
  }
  users {
    bigint id PK
    varchar email UK
    varchar display_name
    varchar provider "GITHUB or GOOGLE"
    varchar provider_user_id
    varchar role "USER or ADMIN"
    char5 zip_code FK "null until set (S3)"
    int radius_miles "default search distance (S3)"
    timestamptz created_at
  }
  shelters {
    bigint id PK
    varchar name
    varchar address
    char5 zip_code FK
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
    int age_months
    text description
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
    text message
    timestamptz created_at
  }
```

## Owners

| Table | Owner | Stories |
|---|---|---|
| `users` | Role 1 (auth and accounts) | S1–S4 |
| `shelters`, `shelter_staff`, `adoption_posts` | Role 2 (shelters and posts) | S6, S7, S10 |
| `zip_codes`, `users.zip_code`, `users.radius_miles` | Role 3 (nearby search) | S3, S5 |
| `favorites`, `inquiries` | Role 4 (favorites and inquiries) | S8, S9 |

## Rules

- `users` has a unique key on (`provider`, `provider_user_id`).
- A user is shelter staff for a shelter when a `shelter_staff` row exists. Only an admin adds or
  removes rows (S10).
- Staff can change only posts whose `shelter_id` is one of their shelters (S7).
- Nearby search returns only `AVAILABLE` posts, using the shelter's ZIP from `zip_codes` (S5, S7).
- Deleting a user deletes their `shelter_staff`, `favorites` and `inquiries` rows (S4).
- Deleting a post deletes its `favorites` and `inquiries`.
