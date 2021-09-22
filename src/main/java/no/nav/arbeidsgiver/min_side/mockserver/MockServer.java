package no.nav.arbeidsgiver.min_side.mockserver;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Profile({"local", "labs"})
@Slf4j
@Component
public class MockServer {

    public static final String SERVICE_EDITION = "1";
    public static final String SERVICE_CODE = "4936";
    public static final String ALTINN_PROXY_PATH = "/altinn-rettigheter-proxy/v2/organisasjoner/*";

    @SneakyThrows
    @Autowired
    MockServer(
            @Value("${mock.port}") int port,
            @Value("${digisyfo.narmestelederUrl}") String digisyfoUrl,
            @Value("classpath:mock/narmesteLeder.json") Resource narmesteLederJson,
            @Value("classpath:mock/organisasjoner.json") Resource organisasjonerJson,
            @Value("classpath:mock/rettigheterTilSkjema.json") Resource rettigheterTilSkjemaJson
    ) {
        log.info("starter mockserveren");
        WireMockServer server = new WireMockServer(
                new WireMockConfiguration()
                        .port(port)
                        .extensions(new ResponseTemplateTransformer(true))
                        .notifier(new ConsoleNotifier(true))
        );
        server.stubFor(any(urlPathMatching(ALTINN_PROXY_PATH + ".*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(organisasjonerJson.getInputStream().readAllBytes())
                )
        );
        server.stubFor(get(urlPathMatching(ALTINN_PROXY_PATH + ".*"))
                .withQueryParams(Map.of(
                        "serviceCode", equalTo(SERVICE_CODE),
                        "serviceEdition", equalTo(SERVICE_EDITION)
                ))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(rettigheterTilSkjemaJson.getInputStream().readAllBytes())
                )
        );
        server.stubFor(any(urlPathMatching(new URL(digisyfoUrl).getPath() + ".*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(narmesteLederJson.getInputStream().readAllBytes())
                )
        );

        server.start();
    }
}