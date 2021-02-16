package no.nav.tag.dittNavArbeidsgiver.services.tokenExchange

import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import org.spockframework.spring.SpringBean
import org.spockframework.spring.StubBeans
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.HttpMethod
import org.springframework.test.web.client.MockRestServiceServer
import spock.lang.Specification
import spock.lang.Unroll

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@StubBeans([MultiIssuerConfiguration])
@RestClientTest(
        components = [TokenExchangeClient],
        properties = [
                "tokenX.tokendingsUrl=http://tolkiendjings"
        ]
)
@AutoConfigureWebClient(registerRestTemplate = true)
class TokenExchangeClientTest extends Specification {

    @SpringBean
    ClientAssertionTokenFactory clientAssertionTokenFactory = Mock()

    @Autowired
    TokenExchangeClient client

    @Autowired
    MockRestServiceServer server

    @Value("\${tokenX.tokendingsUrl}")
    String tokendingsUrl

    def "skal kalle tokenx"() {
        given:
        def subjectToken = "314"
        def clientAssertion = "42"
        def token = "trololol";
        1 * clientAssertionTokenFactory.getClientAssertion() >> clientAssertion
        server
                .expect(requestTo(tokendingsUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().formDataContains(
                                [
                                        "subject_token"   : subjectToken,
                                        "client_assertion": clientAssertion,
                                ]
                        )
                )
                .andRespond(withSuccess(token, APPLICATION_JSON))

        when:
        def result = client.exchangeToken(subjectToken)

        then:
        result == token
    }
}
