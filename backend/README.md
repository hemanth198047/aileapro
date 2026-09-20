# Backend (Gradle / Java 21)

Spring Boot services and shared libraries. No Next.js or pnpm here.

| Path | Role |
|---|---|
| `api/` | HTTP API + SSE tutor (`:8090`) |
| `worker/` | Background jobs |
| `executor/` | Sandboxed lab runs (`:8082`) |
| `libs/` | `domain`, `ai`, `security`, `persistence` |
| `scripts/` | Phase walks, backup restore drill |

```bash
cd backend
./gradlew :api:bootRun
./gradlew :api:seed
./gradlew check
```

Database migrations: Flyway under `api/src/main/resources/db/migration/`.  
Curriculum seed YAML: `api/src/main/resources/seed/`.
