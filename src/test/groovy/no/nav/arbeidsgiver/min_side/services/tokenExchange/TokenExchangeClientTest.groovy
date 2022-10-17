package no.nav.arbeidsgiver.min_side.services.tokenExchange

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import org.spockframework.spring.SpringBean
import org.spockframework.spring.StubBeans
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.HttpMethod
import org.springframework.test.web.client.MockRestServiceServer
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@StubBeans([MultiIssuerConfiguration])
@RestClientTest(
        components = [TokenExchangeClient],
        properties = [
                "token.x.privateJwk=imsecretjwk",
                "token.x.clientId=clientId",
                "token.x.issuer=http://tolkiendjings",
                "token.x.tokenEndpoint=http://tolkiendjings/token",
        ]
)
@EnableConfigurationProperties(TokenXProperties)
@AutoConfigureWebClient(registerRestTemplate = true)
class TokenExchangeClientTest extends Specification {

    @SpringBean
    ClientAssertionTokenFactory clientAssertionTokenFactory = Mock()

    @Autowired
    TokenExchangeClientImpl client

    @Autowired
    MockRestServiceServer server

    @Autowired
    TokenXProperties properties
    ObjectMapper mapper = new ObjectMapper();
    def "skal kalle tokenx"() {
        given:
        def subjectToken = "314"
        def clientAssertion = "42"
        def token = new TokenXToken()
        token.setAccess_token("tolrolro")
        1 * clientAssertionTokenFactory.getClientAssertion() >> clientAssertion
        server
                .expect(requestTo(properties.tokenEndpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().formDataContains(
                                [
                                        "subject_token"   : subjectToken,
                                        "client_assertion": clientAssertion,
                                ]
                        )
                )
                .andRespond(withSuccess(mapper.writeValueAsString(token), APPLICATION_JSON))

        when:
        def result = client.exchange(subjectToken, "aud")

        then:
        result.getAccess_token() == token.getAccess_token()
    }
}
