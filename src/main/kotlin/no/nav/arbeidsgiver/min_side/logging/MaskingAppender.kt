package no.nav.arbeidsgiver.min_side.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.core.AsyncAppenderBase
import java.time.Instant

class MaskingAppender: AsyncAppenderBase<ILoggingEvent>() {
    override fun append(event: ILoggingEvent) {
        super.append(object : ILoggingEvent by event {
            override fun getInstant(): Instant = event.instant ?: Instant.now()

            override fun getFormattedMessage(): String? =
                mask(event.formattedMessage)

            override fun getThrowableProxy(): IThrowableProxy? {
                if (event.throwableProxy == null) {
                    return null
                }
                return object : IThrowableProxy by event.throwableProxy {
                    override fun getMessage(): String? =
                        mask(event.throwableProxy.message)
                }
            }

        })
    }

    companion object {
        private val FNR = Regex("""(^|\D)\d{11}(?=$|\D)""")
        fun mask(string: String?): String? {
            return string?.replace(FNR, "$1***********")
        }
    }
}