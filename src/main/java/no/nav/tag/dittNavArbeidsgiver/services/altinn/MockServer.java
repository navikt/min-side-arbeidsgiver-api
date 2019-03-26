package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static java.nio.charset.StandardCharsets.UTF_8;

@Profile({"dev"})
@Slf4j
@Component
public class MockServer {

    private WireMockServer server;
    private AltinnConfig altinnConfig;

    @SneakyThrows
    @Autowired
    MockServer(
            @Value("${altinn.altinnUrl}") String altinnUrl,
            @Value("${mock.port}") int port,
            AltinnConfig altinnConfig
    ) {
        log.info("starter mockserveren");

        this.altinnConfig = altinnConfig;
        this.server = new WireMockServer(port);

        String altinnPath = new URL(altinnUrl).getPath();
        mockOrganisasjoner(altinnConfig, server,altinnPath);
        mockInvalidSSN(altinnConfig, server,altinnPath);
        server.start();
    }

    public static void mockOrganisasjoner(AltinnConfig altinnConfig, WireMockServer server,String altinnPath) {


        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(altinnPath + "/reportees/"))
                .withHeader("X-NAV-APIKEY", equalTo(altinnConfig.getAPIGwHeader()))
                .withHeader("APIKEY", equalTo(altinnConfig.getAltinnHeader()))
                .withQueryParam("ForceEIAuthentication", equalTo(""))
                .withQueryParam("subject", equalTo("00000000000"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(hentStringFraFil("organisasjoner.json"))
                ));
    }
    public static void mockInvalidSSN(AltinnConfig altinnConfig, WireMockServer server,String altinnPath) {
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(altinnPath + "/reportees/"))
                .withHeader("X-NAV-APIKEY", equalTo(altinnConfig.getAPIGwHeader()))
                .withHeader("APIKEY", equalTo(altinnConfig.getAltinnHeader()))
                .withQueryParam("ForceEIAuthentication", equalTo(""))
                .withQueryParam("subject", equalTo("04010100655"))
                .willReturn(WireMock.aResponse().withStatusMessage("Invalid socialSecurityNumber").withStatus(400)
                        .withHeader("Content-Type", "application/octet-stream")
                ));
    }

    @SneakyThrows
    private static String hentStringFraFil(String filnavn) {
        return IOUtils.toString(MockServer.class.getClassLoader().getResourceAsStream("mock/" + filnavn), UTF_8);
    }
}
