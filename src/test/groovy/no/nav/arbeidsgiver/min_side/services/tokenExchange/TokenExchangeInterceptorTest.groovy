package no.nav.arbeidsgiver.min_side.services.tokenExchange

import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import no.nav.arbeidsgiver.min_side.controller.AuthenticatedUserHolder
import org.spockframework.spring.SpringBean
import org.spockframework.spring.StubBeans
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.test.web.client.MockRestServiceServer
import spock.lang.Specification

import static org.springframework.http.HttpHeaders.AUTHORIZATION
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent

@StubBeans([MultiIssuerConfiguration])
@RestClientTest([TokenExchangeInterceptor])
@AutoConfigureWebClient
class TokenExchangeInterceptorTest extends Specification {

    @SpringBean
    TokenExchangeClient tokenExchangeClient = Mock()

    @SpringBean
    AuthenticatedUserHolder authenticatedUserHolder = Mock()

    @Autowired
    TokenExchangeInterceptor interceptor

    @Autowired
    RestTemplateBuilder restTemplateBuilder

    @Autowired
    MockRestServiceServer server

    def "skal berike request med auth header"() {
        given:
        def subjectToken = "314"
        def tokenXtoken = new TokenXToken()
        tokenXtoken.setAccess_token("tolrolro")
        def restTemplate = restTemplateBuilder.additionalInterceptors(interceptor).build()
        server.bindTo(restTemplate).build()
                .expect(requestTo(""))
                .andExpect(header(AUTHORIZATION, "Bearer ${tokenXtoken.getAccess_token()}"))
                .andRespond(withNoContent())

        when:
        restTemplate.delete("")

        then:
        1 * authenticatedUserHolder.getToken() >> subjectToken
        1 * tokenExchangeClient.exchangeToken(subjectToken) >> tokenXtoken

    }
}
