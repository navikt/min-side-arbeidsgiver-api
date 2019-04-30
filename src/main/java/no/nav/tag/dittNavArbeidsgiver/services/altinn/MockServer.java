package no.nav.tag.dittNavArbeidsgiver.services.altinn;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static java.nio.charset.StandardCharsets.UTF_8;

@Profile({"dev"})
@Slf4j
@Component
public class MockServer {

    private AltinnConfig altinnConfig;

    @SneakyThrows
    @Autowired
    MockServer(
            @Value("${altinn.altinnUrl}") String altinnUrl,
            @Value("${mock.port}") int port,
            AltinnConfig altinnConfig,
            @Value("${sts.stsUrl}") String stsUrl,
            @Value("${aktorregister.aktorUrl}") String aktorUrl,
            @Value("${digisyfo.sykemeldteURL}") String sykemeldteUrl
    ) {
        log.info("starter mockserveren");

        this.altinnConfig = altinnConfig;
        WireMockServer server = new WireMockServer(port);

        String altinnPath = new URL(altinnUrl).getPath();
        String stsPath = new URL(stsUrl).getPath();
        String aktorPath = new URL(aktorUrl).getPath();
        String sykemeldtePath = new URL(sykemeldteUrl).getPath();
        mockOrganisasjoner(altinnConfig, server, altinnPath);
        mockInvalidSSN(altinnConfig, server, altinnPath);
        mockSTSResponse(server, stsPath);
        mockAktorResponse(server, aktorPath);
        mockSykemeldingerResponse(server, sykemeldtePath);
        server.start();
    }

    public static void mockOrganisasjoner(AltinnConfig altinnConfig, WireMockServer server, String altinnPath) {


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

    public static void mockInvalidSSN(AltinnConfig altinnConfig, WireMockServer server, String altinnPath) {
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(altinnPath + "/reportees/"))
                .withHeader("X-NAV-APIKEY", equalTo(altinnConfig.getAPIGwHeader()))
                .withHeader("APIKEY", equalTo(altinnConfig.getAltinnHeader()))
                .withQueryParam("ForceEIAuthentication", equalTo(""))
                .withQueryParam("subject", equalTo("04010100655"))
                .willReturn(WireMock.aResponse().withStatusMessage("Invalid socialSecurityNumber").withStatus(400)
                        .withHeader("Content-Type", "application/octet-stream")
                ));
    }

    public static void mockAktorResponse(WireMockServer server, String aktorURL) {
        log.info("mocking sykemeldte");
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(aktorURL))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(hentStringFraFil("aktorer.json"))
                ));
    }

    public static void mockSTSResponse(WireMockServer server, String stsPath) {
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(stsPath))
                .withQueryParam("grant_type", equalTo("client_credentials"))
                .withQueryParam("scope", equalTo("openid"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(hentStringFraFil("STStoken.json"))
                ));


    }

    public static void mockSykemeldingerResponse(WireMockServer server, String sykemeldtePath) {
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(sykemeldtePath))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(hentStringFraFil("sykemeldinger.json"))
                ));
    }

    @SneakyThrows
    private static String hentStringFraFil(String filnavn) {
        return IOUtils.toString(MockServer.class.getClassLoader().getResourceAsStream("mock/" + filnavn), UTF_8);
    }
}
