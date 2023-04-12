package no.nav.arbeidsgiver.min_side.mockserver

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import no.nav.arbeidsgiver.min_side.config.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Profile("local")
@Component
class MockServer(
    @Value("\${mock.port}") port: Int,
    @Value("classpath:mock/organisasjoner.json") organisasjonerJson: Resource,
    @Value("classpath:mock/rettigheterTilSkjema.json") rettigheterTilSkjemaJson: Resource
) {
    private val log = logger()

    init {
        log.info("starter mockserveren")
        val server = WireMockServer(
            WireMockConfiguration()
                .port(port)
                .extensions(ResponseTemplateTransformer(true))
                .notifier(ConsoleNotifier(true))
        )
        server.stubFor(
            WireMock.any(WireMock.urlPathMatching("$ALTINN_PROXY_PATH.*"))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(organisasjonerJson.inputStream.readAllBytes())
                )
        )
        server.stubFor(
            WireMock.get(WireMock.urlPathMatching("$ALTINN_PROXY_PATH.*"))
                .withQueryParams(
                    mapOf(
                        "serviceCode" to WireMock.equalTo(SERVICE_CODE),
                        "serviceEdition" to WireMock.equalTo(SERVICE_EDITION)
                    )
                )
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(rettigheterTilSkjemaJson.inputStream.readAllBytes())
                )
        )
        server.start()
    }

    companion object {
        const val SERVICE_EDITION = "1"
        const val SERVICE_CODE = "4936"
        const val ALTINN_PROXY_PATH = "/altinn-rettigheter-proxy/v2/organisasjoner/*"
    }
}