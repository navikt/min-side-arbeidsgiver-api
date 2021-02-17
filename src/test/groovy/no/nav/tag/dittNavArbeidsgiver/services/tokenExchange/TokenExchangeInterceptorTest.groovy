package no.nav.tag.dittNavArbeidsgiver.services.tokenExchange

import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration
import no.nav.tag.dittNavArbeidsgiver.utils.TokenUtils
import org.spockframework.spring.SpringBean
import org.spockframework.spring.StubBeans
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

@StubBeans([MultiIssuerConfiguration])
@RestClientTest([TokenExchangeInterceptor])
@AutoConfigureWebClient
class TokenExchangeInterceptorTest extends Specification {

    @SpringBean
    TokenExchangeClient tokenExchangeClient = Mock()

    @SpringBean
    TokenUtils tokenUtils = Mock()

    @Autowired
    TokenExchangeInterceptor interceptor

    @Autowired
    RestTemplateBuilder restTemplateBuilder

    @Autowired
    MockRestServiceServer server

    def "skal berike request med auth header"() {
        given:
        def restTemplate = restTemplateBuilder.additionalInterceptors(interceptor).build()
        server.bindTo(restTemplate).build()
                .expect(requestTo("/"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer 42"))
                .andRespond(withNoContent())

        when:
        restTemplate.delete("/")

        then:
        1 * tokenUtils.getTokenForInnloggetBruker() >> "314"
        1 * tokenExchangeClient.exchangeToken("314") >> "42"

    }
}
