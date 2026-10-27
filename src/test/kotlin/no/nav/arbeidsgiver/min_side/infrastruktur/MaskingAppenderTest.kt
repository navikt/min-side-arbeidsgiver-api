package no.nav.arbeidsgiver.min_side.infrastruktur

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.read.ListAppender
import no.nav.arbeidsgiver.min_side.infrastruktur.MaskingAppender.Companion.mask
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaskingAppenderTest {
    @Test
    fun `Masking i hele exception-kjeden`() {
        val fnr = "11223344556"
        val lc = LoggerContext()
        val listAppender = ListAppender<ILoggingEvent>().apply { context = lc; start() }
        val maskingAppender = MaskingAppender().apply { context = lc; appender = listAppender; start() }
        val logger = lc.getLogger("test").apply {
            isAdditive = false
            addAppender(maskingAppender)
        }

        val exception = RuntimeException(
            "topp fnr=$fnr",
            IllegalArgumentException("cause fnr=$fnr", ExceptionMedEgenToString(fnr)),
        ).apply { addSuppressed(IllegalStateException("suppressed fnr=$fnr")) }

        logger.error("feilet", exception)

        val proxy = listAppender.list.single().throwableProxy
        val kjede = generateSequence(listOf(proxy)) { nivå ->
            nivå.flatMap { listOfNotNull(it.cause) + it.suppressed.orEmpty() }.ifEmpty { null }
        }.flatten().toList()

        assertEquals(4, kjede.size)
        kjede.forEach {
            assertFalse(it.message.orEmpty().contains(fnr), "message i ${it.className}")
            assertFalse(it.overridingMessage.orEmpty().contains(fnr), "overridingMessage i ${it.className}")
        }

        val rendret = ThrowableProxyUtil.asString(proxy)
        assertFalse(rendret.contains(fnr), rendret)
        assertTrue(rendret.contains("suppressed fnr=***********"), rendret)
    }

    private class ExceptionMedEgenToString(private val fnr: String) : RuntimeException() {
        override fun toString() = "egen toString fnr=$fnr"
    }

    @Test
    fun `Masking fødselsnummer`() {
        // works for 11 digits
        assertFalse(mask("At nummer 11223344556")!!.contains("11223344556"))

        // works 12 numbers
        assertTrue(mask("At nummer 112233445566")!!.contains("112233445566"))

        // works fnr leading
        assertFalse(mask("At nummer x11223344556")!!.contains("11223344556"))

        // works fnr followed by char
        assertFalse(mask("At nummer 11223344556x")!!.contains("11223344556"))

        // works fnr:
        assertFalse(mask("fnr:11223344556")!!.contains("11223344556"))

        // works fnr=
        assertFalse(mask("fnr=11223344556")!!.contains("11223344556"))
        assertEquals("fnr=***********", mask("fnr=11223344556"))
        assertEquals("fnr=***********lolwat", mask("fnr=11223344556lolwat"))

        // works fnr with space
        assertEquals("fnr=***********", mask("fnr=112233 44556"))

        // works epost=
        assertFalse(mask("wat=navn@domene.no")!!.contains("navn@domene.no"))
        assertFalse(mask("wat=navn12@domene.no")!!.contains("navn12@domene.no"))
        assertEquals("wat=********&noeannet", mask("wat=navn12@domene.no&noeannet"))

        // works altinn error message
        assertEquals(
            "The ReceiverAddress/User profile must contain a valid emailaddress. Address: ********, User: *********",
            mask("The ReceiverAddress/User profile must contain a valid emailaddress. Address: julenissen@nordpoolen.no, User: 123123123")
        )
    }

    @Test
    fun `Masking password in connection strings`() {
        // works for jdbc:url
        mask(
            "jdbc:postgresql://127.0.0.1:5432/bruker-model?user=notifikasjon-bruker-api&password=foobar&socketFactory=com.google.cloud.sql.postgres.SocketFactory&cloudSqlInstance=lol2%3Anorth-pole1%3Anotifikasjon-bruker-api"
        ).let {
            assertFalse(it!!.contains("foobar"))
            assertEquals(
                "jdbc:postgresql://127.0.0.1:5432/bruker-model?user=notifikasjon-bruker-api&password=********",
                it
            )
        }
    }
}
