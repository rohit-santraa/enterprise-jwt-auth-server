# ADR-002: Database Seeding via CommandLineRunner

**Date:** 2026-06-13
**Status:** Accepted

---

## Context

To exercise Role-Based Access Control we need the canonical roles (`ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`) and at least one admin account present in the database at startup. There is no public endpoint to create an admin (registration only ever assigns `ROLE_USER`), so the admin must be provisioned out-of-band.

Two candidate mechanisms were considered:

1. **`data.sql` / `import.sql`** — a static SQL script run by Spring/Hibernate at boot.
2. **`CommandLineRunner` bean** — Java code that runs once the application context is ready.

---

## Decision

We seed data with a `DataInitializer` bean that implements `CommandLineRunner`
(`config/DataInitializer.java`), not a SQL script.

The runner:
- Seeds every `ERole` value if absent (idempotent via `findByName(...).orElseGet(...)`).
- Seeds an admin (`admin@authserver.com`, roles `ROLE_ADMIN` + `ROLE_USER`) and a regular user (`user@authserver.com`, role `ROLE_USER`) only if they do not already exist.
- Hashes both passwords through the application's `PasswordEncoder` (BCrypt) bean.

---

## Rationale

| Concern | `data.sql` | `CommandLineRunner` (chosen) |
|---|---|---|
| **BCrypt password** | Must hardcode a precomputed hash — opaque, easy to get wrong, can't reuse the app's encoder | Calls the real `PasswordEncoder` bean — always matches the login path |
| **Idempotency** | Requires DB-specific `INSERT ... ON DUPLICATE`/`MERGE` SQL | Plain `existsBy...` / `findBy...` guards in portable Java |
| **Schema timing** | With `ddl-auto: update`, ordering of script vs. Hibernate schema generation is fragile | Runs after the context (and schema) is fully initialised |
| **Maintainability** | Diverges from the entity/repository layer | Reuses the same repositories and entities as production code |

The decisive factor is the BCrypt requirement (CLAUDE.md mandates `BCryptPasswordEncoder` for all stored passwords): a static SQL script cannot hash a password, so it would force a brittle hardcoded hash. The Java runner reuses the exact encoder the login flow uses.

---

## Consequences

- The default seeded credentials are well-known and **must not** ship to production untouched. A follow-up hardening task (Phase 9) should gate seeding behind a non-prod Spring profile or externalise the admin password.
- Seeding logic lives in the application and runs on every boot; the idempotency guards keep this cheap and side-effect-free after the first run.
