//package no.nav.arbeidsgiver.min_side.services.tokenExchange
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Test
//import org.mockito.Mockito.`when`
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.context.properties.EnableConfigurationProperties
//import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
//import org.springframework.http.HttpMethod
//import org.springframework.http.MediaType.APPLICATION_JSON
//import org.springframework.test.context.bean.override.mockito.MockitoBean
//import org.springframework.test.web.client.MockRestServiceServer
//import org.springframework.test.web.client.match.MockRestRequestMatchers.*
//import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
//
//@RestClientTest(
//    components = [TokenExchangeClient::class],
//    properties = [
//        "token.x.privateJwk=imsecretjwk",
//        "token.x.clientId=clientId",
//        "token.x.issuer=http://tolkiendjings",
//        "token.x.tokenEndpoint=http://tolkiendjings/token",
//    ]
//)
//@EnableConfigurationProperties(TokenXProperties::class)
//class TokenExchangeClientTest {
//
//    @MockitoBean
//    lateinit var clientAssertionTokenFactory: ClientAssertionTokenFactory
//
//    @Autowired
//    lateinit var client: TokenExchangeClientImpl
//
//    @Autowired
//    lateinit var server: MockRestServiceServer
//
//    @Autowired
//    lateinit var properties: TokenXProperties
//
//    val mapper = ObjectMapper()
//
//    @Test
//    fun `skal kalle tokenx`() {
//        val subjectToken = "314"
//        val clientAssertion = "42"
//        val token = TokenXToken()
//        token.access_token = "tolrolro"
//
//        `when`(clientAssertionTokenFactory.clientAssertion).thenReturn(clientAssertion)
//        server.expect(requestTo(properties.tokenEndpoint))
//            .andExpect(method(HttpMethod.POST))
//            .andExpect(
//                content().formDataContains(
//                    mapOf(
//                        "subject_token" to subjectToken,
//                        "client_assertion" to clientAssertion,
//                    )
//                )
//            )
//            .andRespond(withSuccess(mapper.writeValueAsString(token), APPLICATION_JSON))
//
//        val result = client.exchange(subjectToken, "aud")
//
//        assertEquals(token.access_token, result.access_token)
//    }
//}