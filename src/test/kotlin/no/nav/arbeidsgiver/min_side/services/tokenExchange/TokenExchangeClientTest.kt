package no.nav.arbeidsgiver.min_side.services.tokenExchange

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import no.nav.arbeidsgiver.min_side.FakeApi
import no.nav.arbeidsgiver.min_side.FakeApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class TokenExchangeClientTest {
    companion object {
        @RegisterExtension
        val app = FakeApplication(
            addDatabase = true,
        ) {
            dependencies {
                provide<TokenExchangeClient>(TokenExchangeClientImpl::class)
                provide<TokenXProperties>(TokenXProperties::class)
                provide<ClientAssertionTokenFactory> { Mockito.mock<ClientAssertionTokenFactory>() }
            }
        }

        @RegisterExtension
        val fakeApi = FakeApi()
    }

    val mapper = ObjectMapper()

    @Test
    fun `skal kalle tokenx`() = app.runTest {
        val subjectToken = "314"
        val clientAssertion = "42"
        val token = TokenXToken()
        token.access_token = "tolrolro"

        `when`(app.getDependency<ClientAssertionTokenFactory>().clientAssertion).thenReturn(clientAssertion)
        fakeApi.registerStub(
            HttpMethod.Post,
            "/token"
        ) {
            val body = call.receive<Map<String, List<String>>>()
            assert(body["subject_token"]!!.contains(subjectToken))
            assert(body["client_assertion"]!!.contains(clientAssertion))
            call.response.headers.append(HttpHeaders.ContentType, "application/json")
            call.respond(mapper.writeValueAsString(token))
        }


        val tokenXClient = app.getDependency<TokenExchangeClient>()

        val result = tokenXClient.exchange(subjectToken, "aud")
        assertEquals(token.access_token, result.access_token)
    }
}