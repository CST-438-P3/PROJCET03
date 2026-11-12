-- First schema for Furever Near, from docs/erd.md.
-- Lengths and allowed values match openapi.yaml. Change this file only before it reaches main;
-- after that, add a new V2__, V3__ ... migration instead.
-- Ids are GENERATED ALWAYS: the database picks every id, so seed data and tests must not insert one.

CREATE TABLE zip_codes (
    zip         VARCHAR(5)       PRIMARY KEY CHECK (zip ~ '^[0-9]{5}$'),
    latitude    DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude   DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180)
);

-- users and identities follow Appendix I, section 3.5: one user, one row per way they sign in.
-- display_name can be empty because some GitHub accounts have no name.
CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(320) NOT NULL,
    display_name  VARCHAR(100),
    role          VARCHAR(5)   NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    zip_code      VARCHAR(5)   REFERENCES zip_codes (zip),
    radius_miles  INTEGER      NOT NULL DEFAULT 10 CHECK (radius_miles BETWEEN 1 AND 100),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- provider_user_id is Google's 'sub' or GitHub's numeric id, never the email.
CREATE TABLE identities (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider          VARCHAR(6)   NOT NULL CHECK (provider IN ('GITHUB', 'GOOGLE')),
    provider_user_id  VARCHAR(255) NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_user_id)
);

CREATE TABLE shelters (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(200) NOT NULL,
    zip_code    VARCHAR(5)   NOT NULL REFERENCES zip_codes (zip),
    phone       VARCHAR(30),
    email       VARCHAR(320),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Adopters need at least one way to reach the shelter.
    CHECK (phone IS NOT NULL OR email IS NOT NULL)
);

CREATE TABLE shelter_staff (
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    shelter_id  BIGINT      NOT NULL REFERENCES shelters (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, shelter_id)
);

CREATE TABLE adoption_posts (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    shelter_id   BIGINT        NOT NULL REFERENCES shelters (id) ON DELETE CASCADE,
    name         VARCHAR(60)   NOT NULL CHECK (btrim(name) <> ''),
    species      VARCHAR(3)    NOT NULL CHECK (species IN ('CAT', 'DOG')),
    breed        VARCHAR(60),
    sex          VARCHAR(7)    NOT NULL DEFAULT 'UNKNOWN' CHECK (sex IN ('MALE', 'FEMALE', 'UNKNOWN')),
    size         VARCHAR(6)    CHECK (size IN ('SMALL', 'MEDIUM', 'LARGE')),
    age_months   INTEGER       CHECK (age_months BETWEEN 0 AND 360),
    description  VARCHAR(2000),
    photo_url    VARCHAR(500),
    status       VARCHAR(9)    NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'ADOPTED')),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE favorites (
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    post_id     BIGINT      NOT NULL REFERENCES adoption_posts (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, post_id)
);

CREATE TABLE inquiries (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    post_id     BIGINT        NOT NULL REFERENCES adoption_posts (id) ON DELETE CASCADE,
    message     VARCHAR(1000) NOT NULL CHECK (btrim(message) <> ''),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- One account per email, whatever the letter case ("A@x.com" and "a@x.com" are the same person).
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));

-- Keeps adoption_posts.updated_at right on every edit, even if the code forgets to set it.
CREATE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER adoption_posts_updated_at
    BEFORE UPDATE ON adoption_posts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_identities_user ON identities (user_id);
CREATE INDEX idx_shelter_staff_shelter ON shelter_staff (shelter_id);
CREATE INDEX idx_shelters_zip ON shelters (zip_code);
CREATE INDEX idx_posts_shelter ON adoption_posts (shelter_id);
CREATE INDEX idx_favorites_post ON favorites (post_id);
CREATE INDEX idx_inquiries_post ON inquiries (post_id);
CREATE INDEX idx_inquiries_user ON inquiries (user_id);
