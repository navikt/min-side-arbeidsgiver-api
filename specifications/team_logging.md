# Team logging for Kafka consumers and userInfo

## 1. Goal

Ship a team-logs pipeline so we can debug and trace support cases where users
claim their accesses are wrong. Specifically:

- Add a separate `team-logs` log stream alongside the existing stdout/Loki
  stream, gated behind an SLF4J `Marker`. Anything logged with that marker goes
  to `team-logs` only and is excluded from the regular logs (and the other way
  around).
- From every Kafka consumer in `MsaKafkaConsumer.kt`, emit a team-log line per
  processed record with enough detail (topic, partition, offset, key, raw
  value) to reproduce what the consumer saw.
- From `UserInfoService.getUserInfoV3`, emit a team-log line per call with the
  resolved user identity and the full response payload that was returned to the
  caller, so we can correlate "user reports their access list is wrong" with
  what we actually computed.

The team-log infrastructure must be a faithful copy of
[`Logging.kt`](../../arbeidsgiver-altinn-tilganger/src/main/kotlin/no/nav/fager/infrastruktur/Logging.kt)
in `arbeidsgiver-altinn-tilganger`, including the explicit `MarkerLogger`, the
`MaskingAppender`, and the per-line character-truncation regex on the
`team-logs` encoder.

## 2. Why a separate team-logs stream

Loki/the default stdout stream is for ops. It is sampled, indexed, and shared
broadly. `team-logs` is a separate destination managed by NAIS, retained
shorter, scoped to the team, and is the right place to put payloads with
end-user content (org numbers, access strings, fnr-shaped data after masking).
Mixing the two would either pollute ops dashboards or leak more than ops is
allowed to see.

The separation is enforced at the appender level by an SLF4J `Marker`
(`TEAM_LOGS`):

- Console/stdout appender: filters out events that carry the marker.
- Logstash TCP appender to `team-logs.nais-system:5170`: filters out events
  that do **not** carry the marker.

A `MarkerLogger` wrapper guarantees that every call made through `teamLogger()`
attaches the marker — direct `marker` arguments are explicitly forbidden so it
is not possible to accidentally bypass the filter.

**Masking is asymmetric and intentional.** In the source `Logging.kt` the
`MaskingAppender` wraps only the `ConsoleAppender`; the `LogstashTcpSocketAppender`
to `team-logs.nais-system:5170` is added directly to the root logger and is
**not** wrapped. Team-logs therefore receives raw, unmasked content. This is
exactly what we want here: the whole reason we are setting team-logs up is to
investigate "this user got the wrong accesses" tickets, and that investigation
needs the actual fnr / org numbers / access strings. The masking on stdout
prevents the same content from leaking into ops/Loki. Do not add masking to
the team-logs appender.

## 3. Current state

- `src/main/kotlin/no/nav/arbeidsgiver/min_side/infrastruktur/Logging.kt` is a
  one-liner that exposes `inline fun <reified T : Any> T.logger()`.
- `src/main/resources/logback.xml` declares a `ConsoleAppender` with
  `LogstashEncoder` and an inline `MaskingJsonGeneratorDecorator` that masks
  11- and 9-digit sequences.
- There is no `META-INF/services/ch.qos.logback.classic.spi.Configurator` and
  no team-logs destination.
- `Miljø.clusterName` already reads `NAIS_CLUSTER_NAME` and falls back to
  `"local"`. There is also `Miljø.resolve(prod, dev, other)`.
- `pom.xml` already has `slf4j-api 2.0.12`, `logback-classic 1.5.22`,
  `logstash-logback-encoder 8.1` — same versions as the source repo. **No new
  dependencies are required.**
- Existing call sites that use `logger()`:
  - [`MsaKafkaConsumer.kt:35`](../src/main/kotlin/no/nav/arbeidsgiver/min_side/services/digisyfo/MsaKafkaConsumer.kt)
  - [`AltinnTilgangSoknadService.kt:11`](../src/main/kotlin/no/nav/arbeidsgiver/min_side/tilgangssoknad/AltinnTilgangSoknadService.kt)

These call sites must keep working unchanged — `logger()` keeps the same
signature.

## 4. Deliverables

### 4.1 Replace `infrastruktur/Logging.kt`

Path: `src/main/kotlin/no/nav/arbeidsgiver/min_side/infrastruktur/Logging.kt`

Port the entire contents of
`arbeidsgiver-altinn-tilganger/src/main/kotlin/no/nav/fager/infrastruktur/Logging.kt`,
adjusting only what is necessary to fit this codebase. Specifically:

1. Change the package declaration to
   `no.nav.arbeidsgiver.min_side.infrastruktur`.
2. Replace `import no.nav.fager.infrastruktur.NaisEnvironment.clusterName`
   with the existing `Miljø.clusterName` from this project. The `LogConfig`
   should read `Miljø.clusterName.isNotEmpty() && Miljø.clusterName != "local"`
   to decide whether to attach the team-logs TCP appender (the source repo
   uses `clusterName.isNotEmpty()`; we use the stricter check because our
   fallback is `"local"`, not `""`).
3. Replace the `basedOnEnv(prod = ..., dev = ..., other = ...)` call inside
   `LogConfig.configure` with the equivalent `Miljø.resolve(prod = { ... },
   dev = { ... }, other = { ... })` already in this project. For now keep all
   three branches at `Level.INFO`, matching the source.
4. Keep the existing `inline fun <reified T : Any> T.logger(): Logger` so the
   two existing call sites compile unchanged. The source repo's variant uses
   `T::class.qualifiedName` while ours uses `this::class.java`. **Keep our
   current form** to avoid silently changing logger names on existing call
   sites; only add `teamLogger()` and the rest as new API.
5. Port verbatim:
   - `const val TEAM_LOGS = "TEAM_LOGS"`
   - `val TEAM_LOG_MARKER: Marker = MarkerFactory.getMarker(TEAM_LOGS)`
   - `class LogConfig : ContextAwareBase(), Configurator` including the two
     filters (console DENY when marker present, TCP ACCEPT only when marker
     present), the custom-fields JSON, and the
     `LoggingEventPatternJsonProvider` whose pattern is
     `{"message":"%replace(%message){'^(.{125000}).+$', '$1...truncated'}"}`.
     This is the per-line character-truncation regex called out in the brief
     and must be preserved exactly — `team-logs` rejects oversized lines, so
     truncation must happen client-side before the line leaves the pod.
   - `private fun <T> T.setup(...)` extension.
   - `class MaskingAppender` with the four regexes (`FNR`, `ORGNR`, `EPOST`,
     `PASSWORD`) and the `mask(...)` companion. It wraps the
     `ConsoleAppender` only (i.e. `MaskingAppender { appender = ConsoleAppender { ... } }`).
     The `LogstashTcpSocketAppender` for team-logs is added to the root
     logger directly and remains unmasked — preserve this asymmetry exactly.
   - `inline fun <reified T> T.teamLogger(): Logger`.
   - `class MarkerLogger(val logger: Logger, val marker: Marker) : Logger` in
     full — including all the `directMarkerUsageNotAllowed()` overrides. Do
     not abridge it; the whole point of this class is that it is exhaustive.

Do **not** keep the `MaskingJsonGeneratorDecorator`-based masking from the
current `logback.xml`. The new `MaskingAppender` replaces it for both streams.

### 4.2 Register the configurator

Create `src/main/resources/META-INF/services/ch.qos.logback.classic.spi.Configurator`
with the single line:

```
no.nav.arbeidsgiver.min_side.infrastruktur.LogConfig
```

### 4.3 Remove `src/main/resources/logback.xml`

Logback uses XML config in preference to a `Configurator` SPI when both are
present, so the file must be deleted (not just emptied) for `LogConfig` to
take effect. Also remove `logback-test.xml` if any test variant exists (none
currently does — verify).

### 4.4 Update both NAIS manifests

Both `nais/dev-env.yaml` and `nais/prod-env.yaml` need:

- Under `accessPolicy.outbound.rules`, add:
  ```yaml
  - application: logging
    namespace: nais-system
  ```
  Without this, the TCP socket to `team-logs.nais-system:5170` is blocked by
  network policy and the appender silently drops events.

- Leave `observability.logging.destinations: [loki]` as-is. The team-logs
  stream is established via the direct TCP appender, not via the NAIS logging
  destinations list.

No changes to `prometheus`, `tokenx`, `azure`, `kafka`, or other sections.

### 4.5 Kafka consumer team logging

File: `src/main/kotlin/no/nav/arbeidsgiver/min_side/services/digisyfo/MsaKafkaConsumer.kt`

Add a class-level team logger alongside the existing one:

```kotlin
private val log = logger()
private val teamLog = teamLogger()
```

For each record processed in `consume(...)` and `batchConsume(...)`, emit a
single `INFO`-level team-log line per record, **before** delegating to
`processor.processRecord(...)` / `processor.processRecords(...)`. The line
must include:

- topic
- partition
- offset
- timestamp (`record.timestamp()`)
- key (string, may be `null`)
- value (raw string, may be `null` for tombstones)
- consumer group id (from `config.groupId`)

Use a structured-friendly format that survives JSON encoding intact. Prefer
the `.also { teamLog.info(...) }` form to keep the call site terse — apply it
inside the existing per-record loop in `consume(...)`:

```kotlin
for (record in records) {
    record.also {
        teamLog.info(
            "kafka record received groupId={} topic={} partition={} offset={} timestamp={} key={} value={}",
            config.groupId, it.topic(), it.partition(), it.offset(),
            it.timestamp(), it.key(), it.value()
        )
    }
    try {
        processor.processRecord(record)
    } catch (e: Exception) {
        // ... existing error handling unchanged ...
    }
}
```

In `batchConsume`, log per record inside a `for (record in records) { ... }`
loop **before** calling `processor.processRecords(records)` (do not log a
single batch summary instead of per-record lines — we need per-record
granularity to debug "this specific event was skipped"). Same `.also { ... }`
form.

Keep the existing `log.info(...)` lines (subscribed, polled count, committing
offsets) untouched — they go to ops/Loki and are useful in their own right.
Keep `log.error(...)` on failure paths untouched. Do **not** mirror the error
to team logs unless the original record content would otherwise be lost; the
per-record team log already captures the input that triggered the failure.

Note: team-logs is unmasked, so the raw `value` (and any org numbers it
contains) lands in team-logs as-is. That is the point — we want to be able
to reproduce exactly what the consumer saw. The same record never reaches
stdout/Loki, so the unmasked content does not leak into ops.

### 4.6 UserInfo team logging

File: `src/main/kotlin/no/nav/arbeidsgiver/min_side/userinfo/UserInfoService.kt`

Add a class-level team logger and emit one team-log line per
`getUserInfoV3(...)` call, on the result of `UserInfoV3.from(...)`. Use
`.also { teamLog.info(...) }` chained on the constructor call so the return
expression stays a single statement. The line must include:

- `fnr` (the input — logged raw; team-logs is unmasked by design and we want
  to be able to grep team-logs by fnr when a user reports their accesses are
  wrong)
- `altinnError`, `digisyfoError` flags
- the full `UserInfoV3` payload, JSON-encoded via `defaultJson` — this gives
  us `tilganger`, `organisasjoner` (each `AltinnTilgang`), `digisyfoOrganisasjoner`
  and `refusjoner` in one shot, in exactly the shape the caller received

Implementation sketch:

```kotlin
private val teamLog = teamLogger()

suspend fun getUserInfoV3(fnr: String, token: String) = supervisorScope {
    // ... existing async/await ...
    UserInfoV3.from(tilganger.await(), syfoVirksomheter.await(), refusjoner.await())
        .also {
            teamLog.info(
                "userInfo response fnr={} altinnError={} digisyfoError={} payload={}",
                fnr, it.altinnError, it.digisyfoError,
                defaultJson.encodeToString(UserInfoV3.serializer(), it)
            )
        }
}
```

Use `defaultJson` from
`no.nav.arbeidsgiver.min_side.infrastruktur.SerDes` (the same instance the
rest of the project uses). All three of `AltinnTilgang`, `VirksomhetOgAntallSykmeldte`,
and `Statusoversikt` are already `@Serializable` because `UserInfoV3` is
serialized as the HTTP response.

Log on the success path only — failures already surface as `altinnError`/
`digisyfoError` flags inside the payload, which is the information ops cares
about. Do not log inside the `runCatching { ... }` blocks; those have already
been folded into the result by the time we want to log.

The 125 000-character truncation regex on the team-logs encoder protects us
from a pathological response with thousands of orgs blowing up team-logs
ingestion. We rely on it; do not add another truncation in code.

### 4.7 Port `TeamLogTest`

Path: `src/test/kotlin/no/nav/arbeidsgiver/min_side/infrastruktur/TeamLogTest.kt`

Copy
[`TeamLogTest.kt`](../../arbeidsgiver-altinn-tilganger/src/test/kotlin/no/nav/fager/infrastruktur/TeamLogTest.kt)
verbatim, changing only the `package` declaration to
`no.nav.arbeidsgiver.min_side.infrastruktur`. Keep all three tests:

1. `vanlig logg skal ikke inneholde verdier i teamLog` — captures stdout while
   logging one ordinary line and one team-log line, asserts the ordinary line
   appears on stdout and the team-log line does not. **This is the
   load-bearing test**: if it fails, the marker filter on the `ConsoleAppender`
   has been broken and team-log content is leaking into ops/Loki. The KDoc on
   the test already says as much; keep the comment.
2. `logging av placeholders, mdc og exceptions fungerer som normalt` — sanity
   check that placeholder substitution, MDC, and exception logging still work
   on the regular logger after the configurator rewrite.
3. `logging av object som arg fungerer` — the `MarkerLogger` overrides
   varargs methods carefully; this guards against a regression where passing
   an object as the SLF4J argument throws.

The private `captureStdout` helper is part of the file — copy it as-is. The
test relies on the configurator being picked up via the SPI file (4.2), so
a passing test also implicitly verifies the META-INF wiring.

### 4.8 Application startup line (optional, recommended)

In `Application.kt`, after the logger is initialised on startup, emit a
single line:

```kotlin
teamLogger().info("Team logging enabled")
```

This mirrors what `arbeidsgiver-altinn-tilganger` does and gives us a positive
signal in `team-logs` per pod start so we can confirm the pipeline is live.
Place it next to the existing `log.info(...)` startup lines (search for
`Application.kt` for an analogous spot).

## 5. Acceptance criteria

1. `mvn -q package` succeeds, including the new `TeamLogTest`. No new compiler
   warnings introduced by `MarkerLogger`'s `@Deprecated`/`Marker?` overrides
   (these are expected and present in the source repo).
2. Existing unit and integration tests pass. The two existing `logger()` call
   sites compile unchanged.
3. Boot the app locally with `NAIS_CLUSTER_NAME` unset. Log lines on stdout
   are JSON and masked (11- and 9-digit sequences redacted on the **stdout**
   stream). The `team-logs.nais-system:5170` socket is **not** opened (verify
   in `LogConfig` that the TCP appender is conditional on `clusterName`).
4. Boot the app locally with `NAIS_CLUSTER_NAME=dev-gcp` (or in dev). The
   stdout stream contains all the existing `log.info(...)` lines and **none**
   of the new `teamLog.info(...)` lines. The team-logs stream (visible in the
   NAIS logging UI / Grafana team-logs board for the `fager` team) contains
   the new per-Kafka-record lines and the per-userInfo-call payload, with
   org numbers and fnrs intact (unmasked).
5. A request to `GET /api/userInfo/v3` produces exactly one new line in
   team-logs per call, containing `payload=` with the full JSON shape.
6. A Kafka record on any of the six configured topics produces exactly one
   new line in team-logs.
7. The 11-digit string `12345678901` appearing in a regular `log.info(...)`
   call comes out as `***********` on stdout. The same string passed through
   `teamLog.info(...)` appears unmasked in team-logs. (The `TeamLogTest`
   covers the routing half of this — the masking half can be eyeballed in a
   dev pod.)

## 6. Out of scope

- Adding team-logs lines to non-Kafka, non-userInfo code paths (Altinn HTTP
  client, Ereg, Refusjon HTTP, etc.). If a future support case needs them,
  add them in a follow-up.
- Changing log levels per package. Everything stays at `INFO`.
- Sampling or rate-limiting. The 125 000-char per-line truncation is the only
  guard for now.
- Replacing `Miljø.clusterName` with a separate `NaisEnvironment` object.
  We reuse what the project already has.
- Migrating other masking patterns (e.g. JWT tokens). The four patterns from
  the source repo (`FNR`, `ORGNR`, `EPOST`, `PASSWORD`) are sufficient and
  match what we mask today.

## 7. Risks and mitigations

- **Risk: team-logs TCP socket fills up on backpressure.** Logback's
  `LogstashTcpSocketAppender` has bounded ring buffers and drops on overflow
  by default — acceptable for debug logs. No code change needed.
- **Risk: team-logs is unmasked, so fnr and org numbers land in team-logs raw.**
  This is intentional — debugging a "wrong accesses" case requires the actual
  identifiers — and team-logs is a team-scoped destination with appropriate
  access controls. The marker-based filter on the `ConsoleAppender` keeps the
  same content off stdout/Loki. The `TeamLogTest` is the regression test that
  guards this boundary; if it ever starts failing, treat it as a sensitive-data
  leak and stop the deploy.
- **Risk: `logback.xml` removal accidentally drops the existing masking and
  the new `LogConfig` is not picked up** (e.g. the META-INF service file is
  misnamed). Mitigation: as part of acceptance, boot locally and grep stdout
  for `***********` in a known fnr-shaped string. If the masking is gone,
  `LogConfig` did not load — most likely the SPI file path is wrong.
- **Risk: logger names change for the two existing `logger()` call sites and
  break log-based alerts.** Mitigation: keep the `this::class.java`
  implementation rather than adopting the source repo's
  `T::class.qualifiedName`. The logger name stays exactly what it is today.

## 8. Files touched (summary)

| Path | Change |
| --- | --- |
| `src/main/kotlin/no/nav/arbeidsgiver/min_side/infrastruktur/Logging.kt` | Replace with ported version |
| `src/main/resources/META-INF/services/ch.qos.logback.classic.spi.Configurator` | New, one line |
| `src/main/resources/logback.xml` | Delete |
| `src/main/kotlin/no/nav/arbeidsgiver/min_side/services/digisyfo/MsaKafkaConsumer.kt` | Add `teamLog`, per-record team-log line in both consume loops |
| `src/main/kotlin/no/nav/arbeidsgiver/min_side/userinfo/UserInfoService.kt` | Add `teamLog`, log full response payload via `.also { ... }` on the `UserInfoV3.from(...)` result |
| `src/test/kotlin/no/nav/arbeidsgiver/min_side/infrastruktur/TeamLogTest.kt` | New, ported verbatim from `arbeidsgiver-altinn-tilganger` (package change only) |
| `src/main/kotlin/no/nav/arbeidsgiver/min_side/Application.kt` | Add `teamLogger().info("Team logging enabled")` on startup |
| `nais/dev-env.yaml` | Add `logging.nais-system` outbound rule |
| `nais/prod-env.yaml` | Add `logging.nais-system` outbound rule |

No `pom.xml` change.
