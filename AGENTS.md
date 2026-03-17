# AGENTS.md

## Project

min-side-arbeidsgiver-api — a Kotlin/Ktor backend for NAV's employer portal ("Min side arbeidsgiver").
Provides APIs for sick leave tracking, organization info, notification status, contact info, saved filters, and more.

Team: fager (@navikt/tag)

## Build & Run

**Prerequisites:** JDK 21, Maven, Docker (for Postgres).

```sh
# Start local Postgres
docker compose up -d

# Init the local database
PGPASSWORD=postgres psql -U postgres -h localhost -p 2345 -f ./local-db-init.sql

# Build and run tests (requires Postgres running on port 2345)
mvn -B package

# Run only tests
mvn -B test

# Run a single test class
mvn -B test -Dtest=KontaktinfoApiTest

# Run the application locally
java -jar target/app.jar
```

Tests require a running PostgreSQL instance. CI uses a Postgres 16.3 Docker service on port 2345
with user/password `postgres/postgres` and a database named `msa` (created by `local-db-init.sql`).

## Project Structure

```
src/
├── main/kotlin/no/nav/arbeidsgiver/min_side/
│   ├── Application.kt              # Entry point, DI config, route registration
│   ├── *Routes.kt                  # HTTP route modules (one file per feature)
│   ├── infrastruktur/              # Cross-cutting: Auth, Database, Http, Health, Metrics, Logging, Cache, SerDes
│   ├── services/
│   │   ├── altinn/                 # Altinn permission/role service
│   │   ├── digisyfo/               # Sykmelding (sick leave) Kafka consumers & repos
│   │   ├── ereg/                   # Organization registry client
│   │   ├── kontaktinfo/            # Contact info client & service
│   │   ├── kontostatus/            # Bank account status client
│   │   ├── lagredefilter/          # Saved filter repository
│   │   └── tiltak/                 # Refund status Kafka consumer
│   ├── sykefravarstatistikk/       # Sick leave statistics
│   ├── tilgangssoknad/             # Altinn access request handling
│   ├── tilgangsstyring/            # Authorization/role checking
│   ├── userinfo/                   # User info service
│   └── varslingstatus/             # Notification status
├── main/resources/
│   ├── db/migration/V*.sql         # Flyway migrations (V1–V15)
│   └── logback.xml
└── test/kotlin/                    # Test suite (mirrors main structure)

nais/
├── dev-env.yaml                    # NAIS app config for dev-gcp
└── prod-env.yaml                   # NAIS app config for prod-gcp

.github/workflows/main.yaml        # CI/CD: test → build → deploy dev → deploy prod
docker-compose.yml                  # Local Postgres for development
local-db-init.sql                   # Creates the "msa" database
local-test.http                     # HTTP requests for manual local testing
```

## Key Architectural Decisions

- **Framework:** Ktor 3 (CIO engine) — not Spring Boot.
- **DI:** `ktor-server-di` plugin. Register with `provide<Interface>(Impl::class)`, resolve with `dependencies.resolve<T>()`.
- **Serialization:** kotlinx.serialization — not Jackson. Use `@Serializable` and the shared `defaultJson` instance.
- **Database:** Raw JDBC with a custom `ParameterSetters` DSL (see `infrastruktur/Database.kt`). No ORM.
- **Kafka:** Coroutine-based polling consumers with manual commit. Each topic has a `RecordProcessor`.
- **Auth:** TokenX for user tokens, Maskinporten for M2M, Azure AD for app auth.
- **Testing:** JUnit 5 + Ktor `testApplication` via `runTestApplication` helper. No mocking libraries — implement interfaces directly.
- **Environment config:** `Miljø.resolve(prod = { }, dev = { }, other = { })` — never hardcode URLs.

## Adding a New Feature

A typical new feature involves:

1. **Data model** — `@Serializable` data classes for request/response.
2. **Database migration** — new `V{n}__description.sql` in `src/main/resources/db/migration/`.
3. **Repository** — data access class using the `Database` DSL.
4. **Service** — business logic, injected with dependencies.
5. **Routes** — `Application.configure*Routes()` extension function in a new `*Routes.kt` file.
6. **DI registration** — add `provide<>()` calls in `Application.configureDependencies()`.
7. **Route installation** — call `configure*Routes()` in the `main` function.
8. **Tests** — API test using `runTestApplication` with DI overrides.

## Conventions

- Norwegian domain terminology, English technical terms.
- All I/O functions must be `suspend`.
- Routes go in `*Routes.kt` files at the package root, not in sub-packages.
- Use `msaApiRouting { }` for authenticated endpoints (auto-applies TokenX auth + base path).
- Flyway migration filenames: `V{number}__{description}.sql` (double underscore).
- Logging via SLF4J. Sensitive data (FNR, org numbers) is masked automatically by logback config.
- No wildcard imports.

## CI/CD

The GitHub Actions workflow (`.github/workflows/main.yaml`) runs on every push:
1. Starts Postgres 16.3 service on port 2345
2. Runs `local-db-init.sql` to create the database
3. `mvn -B package` (compile + test + package)
4. Builds and pushes Docker image to GAR
5. Deploys to dev-gcp (main branch only)
6. Deploys to prod-gcp (main branch only, after dev succeeds)

## Common Pitfalls

- Tests will fail without a running Postgres on port 2345 — always start `docker compose up -d` first.
- The `kotlinx-serialization` compiler plugin is required — it's configured in the Maven kotlin-maven-plugin. Don't forget `@Serializable` on data classes used in JSON.
- `ParameterSetters` auto-increments its index — don't manually specify parameter positions.
- Kafka consumers use manual commit — forgetting to commit will cause reprocessing.
- The `Health.terminate()` signal is checked by background coroutines — new long-running tasks should respect `Health.isActiveAndNotTerminating`.
