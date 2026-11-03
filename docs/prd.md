# PRD: Furever Near

**Team:** Team 1 · **Repo:** https://github.com/CST-438-P3/PROJCET03 · **Last updated:** 2026-11-02

## Users

- **Primary user:** A person who wants to adopt a pet from a shelter near where they live but can
  only travel a limited distance, so they need to see only the pets they could realistically visit.
- **Secondary user:** Shelter staff who post their shelter's adoptable pets and answer inquiries
  about them. An admin grants the shelter role and manages shelters.

## Problem

People who want to adopt spend time scrolling through listings from shelters that are too far away
to visit, and have to check each shelter's website separately to see what is nearby. Large
adoption sites sort by broad region rather than an exact distance the adopter chooses, and they
make it hard to save pets and ask a shelter a question in one place.

## Success measures

- **A new user sees nearby posts in under 2 minutes**, from signing in to setting a ZIP code and
  distance. Timed with at least three people outside the team, on web and Android.
- **Nearby search is 100% accurate:** every returned post is inside the chosen distance and no
  in-range post is missing. Checked by integration and Playwright tests against seed data with
  known distances.
- **A user sends an inquiry from the search results in 3 taps or fewer.** Counted while running
  `docs/demo.md` on web and Android.

## Scope

### MVP

- Account baseline: sign in with GitHub and Google, update profile, delete account (removes the user's data) (S1–S4)
- Nearby search by ZIP code and distance (S5)
- View an adoption post (S6)
- Shelter staff manage their shelter's posts (S7)
- Favorites (S8)
- Inquiries (S9)
- Admin manages shelters and the shelter role (S10)

### Vertical slice

S5: nearby search by ZIP code and distance, end to end on web and Android. The Playwright test
signs in as a seeded user, sets a ZIP code and distance, and checks that only in-range posts
appear, nearest first.

### Not building

- Adoption applications and adoption fee payments (adoption is completed with the shelter, outside the app)
- A map view of results (results are a list sorted by distance)
- Push notifications
- Shelter self-sign-up and verification (an admin grants the shelter role by hand)
- Location from device GPS (users enter a ZIP code)
- An iOS build

## User stories

### S1: Sign in with GitHub

As an adopter, I want to sign in with GitHub so that I do not need a new password.

- **Given** I have a GitHub account with a verified email, **when** I choose "Sign in with GitHub"
  and approve, **then** I am signed in and see the Nearby screen.
- **Given** my GitHub email is not verified, **when** I sign in, **then** I see an error and no
  account is created.

### S2: Sign in with Google

As an adopter, I want to sign in with Google so that I can use the account I already have.

- **Given** I have a Google account with a verified email, **when** I choose "Sign in with Google"
  and approve, **then** I am signed in and see the Nearby screen.
- **Given** my verified email is already registered through GitHub, **when** I sign in with Google,
  **then** I see "This email is registered with GitHub" and no second account is created.

### S3: Update profile and location

As a signed-in user, I want to update my profile, ZIP code and default distance so that searches
start from where I live.

- **Given** I am signed in, **when** I save ZIP code 93933 and distance 10 miles, **then** my
  profile shows them after I reload.
- **Given** I enter ZIP code 00000, **when** I save, **then** I see "We couldn't find that ZIP code"
  and nothing changes.

### S4: Delete account

As a signed-in user, I want to delete my account so that my data is removed.

- **Given** I have favorites and inquiries, **when** I confirm "Delete account", **then** I am
  signed out, and signing in again starts a new, empty account.

### S5: Nearby search

As an adopter, I want to see adoption posts within a distance of my ZIP code so that I only see
pets I can visit.

- **Given** my ZIP code is 93933 and my distance is 10 miles, **when** I open Nearby, **then** I see
  only available posts whose shelter is within 10 miles, nearest first, each showing its distance.
- **Given** no posts are within range, **when** I open Nearby, **then** I see "No pets within 10
  miles" and an option to widen the distance.

### S6: View a post

As an adopter, I want to open a post so that I can see the pet's details and its shelter.

- **Given** I see a post in Nearby, **when** I open it, **then** I see the pet's name, photo,
  species, age, description, shelter name and distance.

### S7: Manage shelter posts

As shelter staff, I want to create, edit, mark adopted and delete posts for my shelter so that the
listings stay current.

- **Given** I have the shelter role for Shelter A, **when** I create a post, **then** it appears in
  Nearby for users in range.
- **Given** a post belongs to Shelter B, **when** I try to edit it, **then** I get "You can't edit
  this post" and it is unchanged.
- **Given** I mark a post adopted, **when** an adopter searches, **then** the post no longer appears.

### S8: Favorites

As an adopter, I want to save posts to favorites so that I can come back to them.

- **Given** I am viewing a post, **when** I tap the heart, **then** it appears in my Favorites list,
  and tapping it again removes it.
- **Given** another user's favorites, **when** I request them, **then** I get 403 or 404.

### S9: Send an inquiry

As an adopter, I want to send an inquiry about a post so that I can ask the shelter a question.

- **Given** I am viewing a post, **when** I send "Is she good with cats?", **then** I see it under
  My inquiries and the shelter staff for that post see it in their inbox.
- **Given** an empty message, **when** I send, **then** I see "Message can't be empty" and nothing
  is sent.

### S10: Admin manages shelters

As an admin, I want to create shelters and grant or revoke the shelter role so that only real
shelter staff can post.

- **Given** I am an admin, **when** I grant a user the shelter role for Shelter A, **then** they can
  create posts for Shelter A.
- **Given** I am not an admin, **when** I call the admin endpoint, **then** I get 403.