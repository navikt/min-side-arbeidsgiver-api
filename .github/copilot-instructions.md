# Copilot Instructions for min-side-arbeidsgiver-api

## Project Overview

This is a Kotlin/Ktor backend API for NAV (Norwegian Labour and Welfare Administration).
It serves the "Min side arbeidsgiver" (My page – employer) frontend, providing employer-facing
APIs for sick leave tracking, organization info, notification status, contact info, and more.

The codebase is in Norwegian where it reflects domain terminology (e.g. `sykmelding`, `arbeidsgiver`,
`tilgangsstyring`). Comments and variable names mix Norwegian domain terms with English technical terms.
Follow this convention when generating code.

## Tech Stack

- **Language:** Kotlin 2.2, targeting JVM 21
- **Framework:** Ktor 3.3 (CIO engine, port 8080)
- **Build:** Maven (pom.xml) — not Gradle
- **Serialization:** kotlinx.serialization (not Jackson)
- **Database:** PostgreSQL 16 via raw JDBC (HikariCP pool), Flyway migrations
- **Messaging:** Apache Kafka (manual commit, coroutine-based consumers)
- **Caching:** Caffeine
- **Auth:** TokenX (citizen tokens), Maskinporten (M2M), Azure AD
- **Monitoring:** Micrometer + Prometheus, Logstash JSON logging
- **Deployment:** NAIS (Kubernetes on GCP), Docker

## Architecture & Patterns

### Dependency Injection

The project uses the `ktor-server-di` plugin. Dependencies are registered in
`Application.configureDependencies()` using `provide<Interface>(ImplementationClass::class)` and
resolved with `dependencies.resolve<Type>()`. Do not introduce Koin, Dagger, or other DI frameworks.

### Route Organization

Routes are defined as `Application` extension functions in separate `*Routes.kt` files at the
package root (`no.nav.arbeidsgiver.min_side`). Each file contains a single
`Application.configure*Routes()` function. Protected endpoints use the `msaApiRouting` helper
which wraps routes under `/ditt-nav-arbeidsgiver-api/api` with TokenX authentication.

Inside routes, use:
- `call.innloggetBruker` to get the authenticated user's FNR (fødselsnummer)
- `call.subjectToken` to get the bearer token for downstream token exchange

### Service / Repository Layer

Two-layer pattern:
1. **Services** — business logic, authorization checks, orchestration of multiple clients
2. **Repositories** — direct database access or Kafka event storage

Services and repositories live in feature-specific packages (e.g. `varslingstatus/`, `services/digisyfo/`).

### Database Access

Raw JDBC with a custom DSL — no ORM (no Exposed, no jOOQ). Key patterns:
- `database.transactional { }` for write operations
- `database.nonTransactionalExecuteQuery(sql, { params }, { resultSet -> mapping })` for reads
- Parameter binding via `ParameterSetters` with typed methods: `text()`, `integer()`, `uuid()`, `nullableText()`, etc.
- Migrations in `src/main/resources/db/migration/V*.sql`

When writing new queries, follow the existing `ParameterSetters` DSL rather than using raw `PreparedStatement.setX()`.

### Kafka Consumers

Kafka consumers run as coroutine-based polling loops in `MsaKafkaConsumer`. Each topic has a
dedicated `RecordProcessor` that implements either `processRecord()` (single) or
`processRecords()` (batch with dedup). Consumers check `Health.isActiveAndNotTerminating` and
perform manual offset commits after successful processing.

### HTTP Clients

Ktor HTTP clients with:
- Automatic retry (3x on 5xx, socket/SSL errors)
- Micrometer metrics (tagged by method, canonicalized URL, status)
- Correlation ID propagation via MDC → `X-Correlation-Id` header
- Token exchange for downstream calls (TokenX or Maskinporten)

### Serialization

Always use `kotlinx.serialization` with `@Serializable` annotations. Custom serializers exist
for UUID, LocalDate, LocalDateTime, and Instant in `infrastruktur/SerDes.kt`. The shared
`defaultJson` instance has `ignoreUnknownKeys = true`.

### Health & Readiness

Services implement `RequiresReady` and register with `Health.register()`. The app exposes
`/internal/isalive`, `/internal/isready`, and `/internal/prometheus`.

### Caching

Use the Caffeine cache utilities in `infrastruktur/Cache.kt`:
- `getOrCompute<K, V>()` for non-null values
- `getOrComputeNullable<K, V>()` for nullable values

## Testing Patterns

Tests use Ktor's `testApplication` via a custom `runTestApplication` helper that accepts
`dependenciesCfg` (for DI overrides) and `applicationCfg` (for installing plugins/routes).

- Mock external services by implementing their interfaces directly in test code
- Use `JSONAssert.assertEquals()` for JSON response assertions
- Test framework: JUnit 5 via `kotlin-test-junit5`
- No mocking library (Mockito/MockK) — use interface implementations
- Database tests use an in-memory or Docker Postgres (see `TestDatabase.kt`)

## Environment Configuration

Environment-specific values use `Miljø.resolve(prod = { }, dev = { }, other = { })`.
The `other` branch provides local development defaults. Never hardcode environment-specific
URLs — always go through `Miljø`.

## Code Style Conventions

- Prefer `suspend` functions for anything involving I/O
- Use `kotlinx.coroutines` for concurrency (not threads)
- Data classes with `@Serializable` for API request/response models
- Extension functions on `Application` for route modules
- Extension properties on `RoutingContext` for auth accessors
- Norwegian domain terms, English technical terms
- No wildcard imports
- Logging via SLF4J (`LoggerFactory.getLogger`)

## What NOT to Do

- Do not introduce Spring Boot — this is a Ktor project
- Do not use Jackson — use kotlinx.serialization
- Do not use an ORM — use the existing JDBC DSL
- Do not add Koin/Dagger — use ktor-server-di
- Do not add MockK/Mockito — implement interfaces directly in tests
- Do not bypass `Miljø` for environment configuration
- Do not use `runBlocking` in production code — use `suspend` functions
- Do not commit secrets or tokens — all secrets come from NAIS environment variables
