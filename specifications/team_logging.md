# Team logging for Kafka consumers and userInfo (v2)

## 1. Goal

Ship a team-logs pipeline so we can debug and trace support cases where users
claim their accesses are wrong. Specifically:

- Add a separate `team-logs` log stream alongside the existing stdout/Loki
  stream, gated behind an SLF4J `Marker`. Anything logged with that marker goes
  to `team-logs` only and is excluded from the regular logs (and the other way
  around).
- From every Kafka consumer in `MsaKafkaConsumer.kt`, emit a team-log line
  **on processing failure** with the full record (topic, partition, offset,
  key, raw value) so we can reproduce what the consumer saw. Do **not** emit a
  team-log line per successful record.
- From `UserInfoService.getUserInfoV3`, emit a team-log summary line on every
  call (counts + error flags), and the full `UserInfoV3` JSON payload only when
  `altinnError || digisyfoError`.

The team-log infrastructure is a port of
[`Logging.kt`](../../arbeidsgiver-altinn-tilganger/src/main/kotlin/no/nav/fager/infrastruktur/Logging.kt)
in `arbeidsgiver-altinn-tilganger`, with explicit hardening on the
`LogstashTcpSocketAppender` (bounded ring buffer, non-blocking append,
non-spinning wait strategy, longer reconnection delay) because v1 fell over.

## 1.1 History: why this is v2

v1 of this spec shipped in
[PR #429](https://github.com/navikt/min-side-arbeidsgiver-api/pull/429) and
was reverted in
[PR #430](https://github.com/navikt/min-side-arbeidsgiver-api/pull/430)
because of:

- **Memory pressure that looked like a leak.** Every Kafka record was logged
  to team-logs with the full raw `value`. With six consumers (some on
  high-volume Sykefravær topics) and the default `LogstashTcpSocketAppender`
  ring buffer of 8 192 slots, each slot retains the `ILoggingEvent` (which in
  turn retains every argument). When the TCP destination stalled or got slow,
  the ring buffer filled and held many MB of `String` payloads — heap climbed,
  pods went OOM. Not a true leak, but the effect was the same.
- **Connection-error spam.** `team-logs.nais-system:5170` is shared
  infrastructure and is not always reachable. The appender's reconnect loop
  produced a steady stream of `java.net.ConnectException` / `SocketException`
  lines, which is noisy and (worse) feeds back into the same logger context.

NAIS' team-logs documentation
([nais.io › observability › logging › team-logs](https://doc.nais.io/observability/logging/how-to/team-logs/))
confirms the TCP appender to `team-logs.nais-system:5170` is the only
supported transport — there is no stdout-based alternative. So v2 keeps the
transport and fixes the application-side mistakes:

1. **Bound the in-process queue and never block the caller.** Smaller ring
   buffer, `appendTimeout = 0`, non-spinning `waitStrategyType` → drop on
   overflow instead of growing the heap, never stall a Kafka poll or HTTP
   handler waiting on the log socket.
2. **Cut the event volume at the source.** Successful Kafka records produce
   nothing on team-logs. `userInfo` produces a small summary on every call
   and the full payload only when something went wrong.
3. **Tame reconnect spam.** Longer reconnection delay, an explicit
   `StatusListener` so logback's internal status output doesn't flood stdout.

Everything else from v1 (marker filter, `MarkerLogger`, `MaskingAppender` on
the console stream only, character-truncation regex, `TeamLogTest`) is kept
as-is.

## 2. Why a separate team-logs stream

Loki/the default stdout stream is for ops. It is sampled, indexed, and shared
broadly. `team-logs` is a separate destination managed by NAIS, retained
shorter, scoped to the team, and is the right place to put payloads with
end-user content (org numbers, access strings, fnr). Mixing the two would
either pollute ops dashboards or leak more than ops is allowed to see.

The separation is enforced at the appender level by an SLF4J `Marker`
(`TEAM_LOGS`):

- Console/stdout appender: filters out events that carry the marker.
- Logstash TCP appender to `team-logs.nais-system:5170`: filters out events
  that do **not** carry the marker.

A `MarkerLogger` wrapper guarantees that every call made through `teamLogger()`
attaches the marker — direct `marker` arguments are explicitly forbidden so it
is not possible to accidentally bypass the filter.

**Masking is asymmetric and intentional.** The `MaskingAppender` wraps only
the `ConsoleAppender`; the `LogstashTcpSocketAppender` is added to the root
logger directly and is **not** wrapped. Team-logs receives raw, unmasked
content — the whole reason we are setting it up is to investigate
"this user got the wrong accesses" tickets, which needs the actual fnr / org
numbers / access strings. Do not add masking to the team-logs appender.

## 3. Current state (post-revert, pre-v2 changes)

The revert of PR #430 put the v1 implementation back on the branch. As of
`HEAD` on `revert-430-rm_team_logs`:

- `src/main/kotlin/no/nav/arbeidsgiver/min_side/infrastruktur/Logging.kt`
  contains the full v1 port (`LogConfig`, `MaskingAppender`, `teamLogger()`,
  `MarkerLogger`). The `LogstashTcpSocketAppender` uses **defaults** for ring
  buffer, append timeout, wait strategy, and reconnection delay — those are
  the v2 hardening targets.
- `src/main/resources/META-INF/services/ch.qos.logback.classic.spi.Configurator`
  exists and points at `LogConfig`. `logback.xml` is gone.
- `MsaKafkaConsumer.kt` logs a team-log line per record in both `consume(...)`
  and `batchConsume(...)`, **including on the success path**. This is the
  volume problem; v2 removes the success-path logs.
- `UserInfoService.kt` always logs the full `UserInfoV3` JSON payload to
  team-logs. v2 changes this to summary-always, full-on-error.
- `nais/dev-env.yaml` and `nais/prod-env.yaml` already have
  `logging.nais-system` in `accessPolicy.outbound.rules`.
- `Application.kt` already emits `teamLogger().info("Team logging enabled")`
  at startup.
- `src/test/kotlin/no/nav/arbeidsgiver/min_side/infrastruktur/TeamLogTest.kt`
  is in place and passing.

So v2 is a focused change — three files, two distinct concerns (TCP appender
hardening + event volume reduction).

## 4. Deliverables

### 4.1 Harden the `LogstashTcpSocketAppender` in `Logging.kt`

File: `src/main/kotlin/no/nav/arbeidsgiver/min_side/infrastruktur/Logging.kt`

Inside `LogConfig.configure`, on the `LogstashTcpSocketAppender().setup(lc) { ... }`
block, set the following properties **in addition to** what is there today.
Do not change anything else in the file (the masking, marker filter, custom
fields, and truncation pattern are correct).

```kotlin
addAppender(LogstashTcpSocketAppender().setup(lc) {
    this.name = "TEAMLOGS"
    addDestination("team-logs.nais-system:5170")

    // --- v2 hardening: bound the queue, never block the caller ---
    this.ringBufferSize = 1024                                                          // default 8192
    this.appendTimeout = ch.qos.logback.core.util.Duration.buildByMilliseconds(0.0)     // drop on full instead of blocking
    setWaitStrategyType("sleeping")                                                     // method (no getter → no synthetic property); no busy spin
    this.reconnectionDelay = ch.qos.logback.core.util.Duration.buildByMinutes(1.0)      // default 30s
    this.keepAliveDuration = ch.qos.logback.core.util.Duration.buildByMinutes(5.0)      // keep socket warm
    // --- end v2 hardening ---

    this.encoder = LogstashEncoder().setup(lc) {
        // ... existing customFields, isIncludeContext = false,
        //     LoggingEventPatternJsonProvider truncation pattern unchanged ...
    }
    addFilter(...)  // existing ACCEPT-on-marker filter unchanged
})
```

Notes on the API (logstash-logback-encoder 8.1):

- `ringBufferSize` is a power-of-two `Int` on `AsyncDisruptorAppender`. 1024 ×
  worst-case ~50 KB per event = ~50 MB retained at full backpressure — well
  inside our 1 GiB pod limit.
- `appendTimeout` expects `ch.qos.logback.core.util.Duration` (logback's
  type, **not** `java.time.Duration`). There is no `Duration.ZERO` constant;
  use `Duration.buildByMilliseconds(0.0)`. A zero-millisecond timeout means
  "do not wait if the ring buffer is full, drop the event and return
  immediately." This is the load-bearing fix — it guarantees that a stalled
  team-logs destination cannot block a Kafka poll loop or an HTTP handler.
- `waitStrategyType` is exposed as `setWaitStrategyType(String)` only — no
  matching getter, so Kotlin does **not** synthesise a property. Call the
  setter as a method: `setWaitStrategyType("sleeping")`. Valid values
  include `"blocking"`, `"sleeping"`, `"yielding"`, `"busySpin"`,
  `"phasedBackoff{...}"`. We pick `"sleeping"` because it does not burn
  CPU when there is nothing to consume.
- `reconnectionDelay` and `keepAliveDuration` also use logback's
  `ch.qos.logback.core.util.Duration`. They do have matching getters, so
  the Kotlin property assignment works. Use the `buildBy*` factory methods
  to avoid relying on the string parser.

### 4.2 Status listener to quiet logback's own connection-error spam

File: `src/main/kotlin/no/nav/arbeidsgiver/min_side/infrastruktur/Logging.kt`

Add this at the top of `LogConfig.configure(lc)`, before any appender is
created:

```kotlin
override fun configure(lc: LoggerContext): ExecutionStatus {
    // Suppress repeated transport-error status messages from the TCP appender.
    // Without this, logback's StatusManager prints a connection error to
    // stderr on every failed reconnect, which is noisy and (in v1) appeared
    // in pod logs as a slow drip even when team-logs was just briefly down.
    lc.statusManager.add(RateLimitedStatusListener(maxMessagesPerMinute = 6))

    // ... existing rootAppender / addAppender code unchanged ...
}
```

And add the class to the same file (alongside `MaskingAppender`):

```kotlin
/**
 * StatusListener that rate-limits status messages to N per minute.
 *
 * Used to silence the reconnect-error storm from LogstashTcpSocketAppender
 * when team-logs.nais-system is briefly unreachable. Without this, every
 * reconnect attempt produces a Status WARN/ERROR that the StatusManager
 * prints to stderr.
 */
class RateLimitedStatusListener(
    private val maxMessagesPerMinute: Int,
) : ch.qos.logback.core.status.StatusListener,
    ch.qos.logback.core.spi.LifeCycle {

    private val window = java.time.Duration.ofMinutes(1)
    private val timestamps = java.util.ArrayDeque<java.time.Instant>()
    private var started = false

    override fun addStatusEvent(status: ch.qos.logback.core.status.Status) {
        // Only rate-limit WARN/ERROR; let INFO through (they are rare, mostly startup).
        if (status.level < ch.qos.logback.core.status.Status.WARN) {
            System.err.println(status)
            return
        }
        val now = java.time.Instant.now()
        synchronized(timestamps) {
            while (timestamps.isNotEmpty() && java.time.Duration.between(timestamps.peekFirst(), now) > window) {
                timestamps.pollFirst()
            }
            if (timestamps.size < maxMessagesPerMinute) {
                timestamps.addLast(now)
                System.err.println(status)
            }
            // else: drop silently
        }
    }

    override fun start() { started = true }
    override fun stop() { started = false }
    override fun isStarted(): Boolean = started
}
```

This is enough. We could go further (e.g. count drops and emit a "suppressed
N messages" summary), but the bar is "do not flood pod stdout with reconnect
exceptions", which six lines/minute satisfies.

### 4.3 Kafka consumer: log on error only

File: `src/main/kotlin/no/nav/arbeidsgiver/min_side/services/digisyfo/MsaKafkaConsumer.kt`

**Remove** the per-record team-log lines on the success path in both
`consume(...)` and `batchConsume(...)` — the `record.also { teamLog.info(...) }`
blocks. **Add** the team-log call inside the existing `catch (e: Exception)`
branch instead.

`consume(...)` shape after the change:

```kotlin
for (record in records) {
    try {
        processor.processRecord(record)
    } catch (e: Exception) {
        teamLog.error(
            "kafka record failed groupId={} topic={} partition={} offset={} timestamp={} key={} value={}",
            config.groupId, record.topic(), record.partition(), record.offset(),
            record.timestamp(), record.key(), record.value(), e
        )
        log.error("Feil ved prosessering av kafka-melding.", e)

        // without seek next poll will advance the offset, regardless of autocommit=false
        consumer.seek(TopicPartition(record.topic(), record.partition()), record.offset())

        throw Exception("Feil ved prosessering av kafka-melding. partition=${record.partition()} offset=${record.offset()} $config", e)
    }
}
```

`batchConsume(...)` shape: the existing failure path catches once per batch,
not per record — we don't know which record in the batch was the culprit. Log
every record in the batch on failure so the support case has the full
context. (Errors are rare; a batch of, say, 500 records logged once on a real
failure is fine.)

```kotlin
try {
    processor.processRecords(records)
} catch (e: Exception) {
    for (record in records) {
        teamLog.error(
            "kafka record failed (batch) groupId={} topic={} partition={} offset={} timestamp={} key={} value={}",
            config.groupId, record.topic(), record.partition(), record.offset(),
            record.timestamp(), record.key(), record.value()
        )
    }
    log.error("Feil ved prosessering av kafka-melding.", e)

    for (tp in records.partitions()) {
        consumer.seek(tp, records.records(tp).first().offset())
    }

    throw Exception("Feil ved prosessering av kafka-melding. $config", e)
}
```

Pass the exception only to the first `teamLog.error` call (or just the
`log.error`), not on every per-record team-log line — repeating the stack
trace per record is wasteful.

The existing `log.info(...)` lines (subscribed, polled count, committing
offsets) stay untouched; they remain on stdout/Loki for ops.

### 4.4 UserInfo: summary always, full payload on error only

File: `src/main/kotlin/no/nav/arbeidsgiver/min_side/userinfo/UserInfoService.kt`

Replace the single `teamLog.info(... payload=...)` inside the `.also { ... }`
with a two-step pattern: always log the small summary, log the full payload
only when one of the error flags is set.

```kotlin
suspend fun getUserInfoV3(fnr: String, token: String) = supervisorScope {
    val tilganger = async { runCatching { altinnTilgangerService.hentAltinnTilganger(token) } }
    val syfoVirksomheter = async { runCatching { digisyfoService.hentVirksomheterOgSykmeldte(fnr) } }
    val refusjoner = async { runCatching { refusjonStatusService.statusoversikt(token) } }

    UserInfoV3.from(tilganger.await(), syfoVirksomheter.await(), refusjoner.await())
        .also { result ->
            teamLog.info(
                "userInfo summary fnr={} altinnError={} digisyfoError={} orgCount={} digisyfoOrgCount={} refusjonCount={} tilgangCount={}",
                fnr, result.altinnError, result.digisyfoError,
                result.organisasjoner.size, result.digisyfoOrganisasjoner.size,
                result.refusjoner.size, result.tilganger.size
            )
            if (result.altinnError || result.digisyfoError) {
                teamLog.info(
                    "userInfo payload (errored) fnr={} payload={}",
                    fnr,
                    defaultJson.encodeToString(UserInfoV3.serializer(), result)
                )
            }
        }
}
```

Rationale:

- The summary line is small (well under 1 KB) and goes on every call. It
  gives us a per-user heartbeat in team-logs: "did this user even hit the
  endpoint, and what did the counts look like."
- The full payload only fires when at least one upstream failed
  (`altinnError` or `digisyfoError`), which is the case where a support
  ticket is plausible anyway. In healthy operation we ship zero full
  payloads, so the byte cost is bounded by the summary line × QPS.
- We deliberately do **not** add a random-sample full-payload log (e.g.
  "1 in 100 successful calls"). If support needs to debug a successful call
  whose contents look wrong to the user, we can re-add sampling — but right
  now the summary is enough to confirm "we returned N orgs" and the user's
  actual access set lives upstream in Altinn anyway.

### 4.5 No changes to v1 deliverables that are already merged

The following are already on the branch in their v1 form and need **no
modification**:

- `META-INF/services/ch.qos.logback.classic.spi.Configurator`
- The `logback.xml` deletion
- `nais/dev-env.yaml` and `nais/prod-env.yaml` outbound rule
  (`logging.nais-system`)
- `Application.kt` `Team logging enabled` startup line
- `TeamLogTest.kt`
- The `MaskingAppender` four-regex masking (plus the `FNR_SPACE` pattern and
  the MDC masking that landed in PR #432 — keep both)

## 5. Acceptance criteria

1. `mvn -q package` succeeds. `TeamLogTest` still passes — the marker filter
   is unchanged. No new compiler warnings beyond the expected `MarkerLogger`
   ones.
2. A `GET /api/userInfo/v3` against a healthy backend produces **one** team-log
   line ending in `userInfo summary fnr=... orgCount=...` and **zero** lines
   with `payload=`.
3. The same endpoint against a backend where `AltinnTilgangerService` is
   forced to fail (or returns `isError = true`) produces **one** summary line
   and **one** `userInfo payload (errored)` line containing the JSON.
4. Driving Kafka traffic on any topic produces **zero** team-log lines per
   successful record. Making `processRecord(...)` throw produces **one**
   `kafka record failed ...` team-log line per failed record, with the raw
   `value` intact (unmasked).
5. In dev, with `team-logs.nais-system` reachable, the new lines appear in
   the team-logs index for the `fager` team and stdout is silent.
6. In dev, with team-logs deliberately blocked (temporarily remove the
   `logging.nais-system` outbound rule, deploy, then put it back), pod
   memory stays under ~512 MiB during sustained Kafka traffic — confirming
   the bounded ring buffer + dropping appender. Reconnect-error lines on
   stderr stay under a handful per minute, confirming the status listener.
7. Sanity check the truncation: emit a `teamLog.info(...)` with a > 125 000
   character argument. The line that lands in team-logs ends in
   `...truncated"` (the JSON-encoded truncation pattern from the encoder).

Criteria 6 and 7 are nice-to-have manual verifications during the next dev
deploy; they are the failure-mode regression tests for v1's two problems.

## 6. Out of scope

- Adding team-logs lines to non-Kafka, non-userInfo code paths.
- Random-sample full-payload userInfo logs. We will reconsider if support
  needs them.
- A Micrometer gauge for ring-buffer fill ratio. Useful follow-up if we want
  early warning of pressure before drops start; not required for v2.
- Tuning `writeBufferSize`, `writeTimeout`, or the TCP keep-alive socket
  options below the JVM defaults. The Logback defaults are fine once the
  application-side queue is bounded.

## 7. Risks and mitigations

- **Risk: we still drop events under sustained backpressure.** Yes — that is
  the point. Team-logs is a debug-only stream; losing some lines is
  preferable to OOMing the pod or stalling Kafka. The error-path Kafka log
  and the userInfo error-path payload log are the only "important" lines,
  and they are emitted from contexts where errors are already rare.
- **Risk: the `appendTimeout = Duration.ZERO` semantics differ between
  versions of logstash-logback-encoder.** As of 8.1 (which we use),
  `Duration.ZERO` documents as "do not wait." If a future upgrade changes
  this, the ring-buffer-full test (criterion 6) will catch it — but the
  failure mode would be "Kafka throughput drops" not "memory leak", so it
  is monitorable.
- **Risk: rate-limited status listener swallows a useful diagnostic.** Cap
  is 6/minute on WARN+. A genuinely broken transport will still produce
  multiple lines per minute for as long as it is broken; we won't miss it.
  INFO-level status events (mainly the lifecycle messages on boot) are not
  rate-limited.
- **Risk: team-logs is unmasked.** Unchanged from v1. The marker filter on
  `ConsoleAppender` keeps the same content off stdout/Loki. `TeamLogTest` is
  the regression test.

## 8. Files touched (v2 only)

| Path | Change |
| --- | --- |
| `src/main/kotlin/no/nav/arbeidsgiver/min_side/infrastruktur/Logging.kt` | Add `ringBufferSize`, `appendTimeout`, `waitStrategyType`, `reconnectionDelay`, `keepAliveDuration` on the TCP appender. Add `RateLimitedStatusListener` class and register it on the `LoggerContext`. |
| `src/main/kotlin/no/nav/arbeidsgiver/min_side/services/digisyfo/MsaKafkaConsumer.kt` | Remove per-record success-path `teamLog.info(...)` in both `consume` and `batchConsume`. Add `teamLog.error(...)` inside the existing failure paths (per-record in `consume`, per-record-in-batch in `batchConsume`). |
| `src/main/kotlin/no/nav/arbeidsgiver/min_side/userinfo/UserInfoService.kt` | Replace single `teamLog.info` with summary line on every call + full-payload line only when `altinnError || digisyfoError`. |

No NAIS manifest changes, no `pom.xml` change, no new files. Existing
`TeamLogTest` continues to cover the marker-routing invariant.
