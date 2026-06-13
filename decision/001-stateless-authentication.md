# ADR-001: Stateless JWT Architecture

**Date:** 2026-06-13
**Status:** Accepted

---

## Context

We are building a central authentication and authorization service intended to be consumed by multiple downstream microservices or frontend clients. We needed to decide how the server manages authenticated session state between requests.

The two primary candidates were:

1. **Stateful sessions** — the server stores session data (in-memory or in a shared store like Redis) and issues a session cookie to the client.
2. **Stateless JWT** — the server issues a signed token containing all necessary claims; the client presents it on every request and the server verifies the signature without consulting any store.

---

## Decision

We chose a **100% stateless architecture** using signed JWTs (via the `jjwt` library) with Spring Security configured as `SessionCreationPolicy.STATELESS`.

- **Access tokens** are short-lived (15–30 minutes) and self-contained, carrying the user's identity and roles as claims.
- **Refresh tokens** are long-lived (7 days), opaque, and persisted in the `refresh_tokens` MySQL table with a `revoked` flag to support explicit logout and revocation.
- `HttpSession` is never created or used anywhere in the application.

---

## Rationale

| Concern | Stateful Sessions | Stateless JWT (chosen) |
|---|---|---|
| Horizontal scaling | Requires sticky sessions or a shared session store (Redis) | No shared state — any instance can validate any token |
| Coupling | Server must maintain session lifecycle | Server is purely a token issuer and validator |
| Revocation | Trivial — delete the session | Handled via short access token TTL + DB-backed refresh token revocation |
| Microservice compatibility | Session cookies don't cross service boundaries cleanly | Bearer tokens travel in the `Authorization` header, language/framework agnostic |
| Operational complexity | Requires a session store and cache invalidation strategy | Only requires a secret key and DB table for refresh tokens |

The enterprise use-case — a central auth server serving heterogeneous clients — makes stateless JWT the clear fit. Horizontal scaling without a shared session store is a hard requirement for production deployments, and JWT satisfies this with no extra infrastructure.

---

## Consequences

- The `JwtAuthenticationFilter` becomes the single gatekeeper for every protected request; it must be robust and well-tested.
- Access tokens cannot be revoked mid-life (before expiry). The 15–30 minute TTL is the accepted trade-off.
- Refresh token revocation (logout) is enforced by flipping the `revoked` column; the `RefreshTokenService` must check this flag on every refresh request.
- The JWT signing secret must be externalized (environment variable / secrets manager) and never hardcoded.
