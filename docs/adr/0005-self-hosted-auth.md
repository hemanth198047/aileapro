# ADR 0005 — Spring Security, self-hosted sessions

## Status

Accepted (replaces Better Auth). See [0008](0008-java-spring-backend.md).

## Decision

Spring Security 6: form/JSON login, Argon2 password hashing, session cookie (httpOnly, Secure, SameSite), CSRF on cookie-auth mutations, method security for RBAC. Spring Session backed by Redis in staging/prod.

OAuth2 (Google, GitHub) in P1 via Spring Security OAuth2 Client.

No Clerk/Auth0 required for P0.
