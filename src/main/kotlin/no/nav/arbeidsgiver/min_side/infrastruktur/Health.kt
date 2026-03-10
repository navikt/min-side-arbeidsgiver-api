package no.nav.arbeidsgiver.min_side.infrastruktur

import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicBoolean

interface RequiresReady {
    fun isReady(): Boolean
}

object Health {
    private val reaquiredServices = mutableListOf<RequiresReady>()

    fun register(requiresReady: RequiresReady) {
        reaquiredServices.add(requiresReady)
    }

    val alive
        get() = true

    val ready
        get() = reaquiredServices.all { it.isReady() }

    private val terminatingAtomic = AtomicBoolean(false)

    val terminating: Boolean
        get() = !alive || terminatingAtomic.get()

    fun terminate() {
        terminatingAtomic.set(true)
    }
}

val CoroutineScope.isActiveAndNotTerminating: Boolean
    get() = isActive && !Health.terminating

fun Application.registerShutdownListener() {
    monitor.subscribe(ApplicationStopping) {
        log.info("ApplicationStopping: signal Health.terminate()")
        Health.terminate()
    }
}