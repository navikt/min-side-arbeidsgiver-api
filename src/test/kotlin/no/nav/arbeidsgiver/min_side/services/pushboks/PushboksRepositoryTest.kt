package no.nav.arbeidsgiver.min_side.services.pushboks

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeidsgiver.min_side.services.pushboks.PushboksRepository.PushboksKey
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("local-kafka")
@SpringBootTest(
    webEnvironment = NONE,
    classes = [
        PushboksRepositoryImpl::class,
        ObjectMapper::class,
    ],
    properties = [
        "spring.flyway.enabled=true",
        "spring.flyway.clean-disabled=false",
        "spring.datasource.url=jdbc:postgresql://localhost:2345/msa?user=postgres&password=postgres",
    ]
)
@Import(value = [
    DataSourceAutoConfiguration::class,
    JdbcTemplateAutoConfiguration::class,
    FlywayAutoConfiguration::class,
])
class PushboksRepositoryTest {

    @Autowired
    lateinit var pushboksRepository: PushboksRepository

    @Autowired
    lateinit var flyway: Flyway

    @BeforeEach
    fun setUp() {
        flyway.clean()
        flyway.migrate()
    }

    @Test
    fun `hent ved tom db gir tom liste`() {
        assertThat(pushboksRepository.hent("42")).isEmpty()
    }

    @Test
    fun `hent for virksomhet returerer relevante bokser`() {
        pushboksRepository.processEvent(PushboksKey("fooService", "42"), """{"foo":"bar"}""")
        pushboksRepository.processEvent(PushboksKey("barService", "44"), """{"foo":"bar"}""")

        val bokser = pushboksRepository.hent("42")
        assertThat(bokser).hasSize(1)
        assertThat(bokser.first().tjeneste).isEqualTo("fooService")
        assertThat(bokser.first().virksomhetsnummer).isEqualTo("42")
        assertThat(bokser.first().innhold.toString()).isEqualTo("""{"foo":"bar"}""")
    }

    @Test
    fun `null event sletter boks`() {
        val key = PushboksKey("fooService", "42")

        pushboksRepository.processEvent(key, "{}")
        assertThat(pushboksRepository.hent("42")).hasSize(1)

        pushboksRepository.processEvent(key, null)
        assertThat(pushboksRepository.hent("42")).hasSize(0)
    }

    @Test
    fun `boks upsertes`() {
        val key = PushboksKey("fooService", "42")

        pushboksRepository.processEvent(key, "{}")
        assertThat(pushboksRepository.hent("42")).hasSize(1)

        pushboksRepository.processEvent(key, """{ "foo": "bar" }""")
        pushboksRepository.hent("42").let { bokser ->
            assertThat(bokser).hasSize(1)
            assertThat(bokser.first().innhold.toString()).isEqualTo("""{"foo":"bar"}""")
        }
    }
}