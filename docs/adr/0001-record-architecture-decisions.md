# 1. Record architecture decisions

- Date: 2026-10-29
- Status: Accepted

## Context
We need a record of significant decisions and why we made them.

## Decision
We keep ADRs in `docs/adr/`, numbered, one decision per file. A superseded ADR stays, with
`Status: Superseded by NNNN`, and its replacement links the code PR that made the change.

## Consequences
Decisions are reviewable in PRs like code.
