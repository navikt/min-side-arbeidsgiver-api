package no.nav.arbeidsgiver.min_side.tilgangssoknad

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.MultiGauge.Row
import io.micrometer.core.instrument.Tags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.arbeidsgiver.min_side.infrastruktur.Metrics
import no.nav.arbeidsgiver.min_side.infrastruktur.isActiveAndNotTerminating
import kotlin.time.Duration.Companion.minutes


/**
 * Eksponerer totalt antall opprettede delegation requests per `resource`, lest fra DB periodisk.
 * Verdien er monotont økende (utenom ved ev. retention-sletting), så tidsintervaller beregnes
 * i Grafana/PromQL, f.eks. daglig: `delta(fager_msa_delegation_request_opprettet_total[1d])`.
 *
 * Prometheus: `fager_msa_delegation_request_opprettet_total{resource="nav_foo"}`
 */
suspend fun Application.startDelegationRequestMetrics(scope: CoroutineScope) {
    val repository = dependencies.resolve<DelegationRequestRepository>()
    val multiGauge = MultiGauge.builder("fager.msa.delegation_request.opprettet")
        .description("totalt antall delegation requests opprettet, per resource")
        .baseUnit("requests")
        .register(Metrics.meterRegistry)

    scope.launch {
        while (isActiveAndNotTerminating) {
            try {
                val counts = repository.tellOpprettetPerResource()
                multiGauge.register(
                    counts.map { (resource, antall) ->
                        Row.of(Tags.of("resource", resource), antall)
                    },
                    /* overwrite = */ true,
                )
            } catch (e: Exception) {
                log.warn("Kunne ikke oppdatere delegation request metrics: {}", e.message)
            }
            delay(5.minutes)
        }
    }
}
