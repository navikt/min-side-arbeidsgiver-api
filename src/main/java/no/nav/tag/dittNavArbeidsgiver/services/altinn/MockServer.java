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
            @Value("${aad.aadAccessTokenURL}") String aadUrl,
            @Value("${aktorregister.aktorUrl}") String aktorUrl,
            @Value("${digisyfo.sykemeldteURL}") String sykemeldteUrl,
            @Value("${digisyfo.syfooppgaveurl}") String syfoOpggaveUrl,
            @Value("${digisyfo.digisyfoUrl}") String digisyfoUrl
    ) {
        log.info("starter mockserveren");

        this.altinnConfig = altinnConfig;
        WireMockServer server = new WireMockServer(port);
        String altinnPath = new URL(altinnUrl).getPath();
        String stsPath = new URL(stsUrl).getPath();
        String aadPath = new URL(aadUrl).getPath();
        String aktorPath = new URL(aktorUrl).getPath();
        String sykemeldtePath = new URL(sykemeldteUrl).getPath();
        String syfoOppgavePath = new URL(syfoOpggaveUrl).getPath();
        String syfoNarmesteLederPath = new URL(digisyfoUrl).getPath();
        mockOrganisasjoner(altinnConfig, server, altinnPath);
        mockInvalidSSN(altinnConfig, server, altinnPath);
        mockRoles(altinnConfig,server,altinnPath);
        mockSTSResponse(server, stsPath);
        mockAadResponse(server, aadPath);
        mockAktorResponse(server, aktorPath);
        mockSykemeldingerResponse(server, sykemeldtePath);
        mockSyfoOppgaverResponse(server, syfoOppgavePath);
        mockSyfoNarmesteLeder(server, syfoNarmesteLederPath);
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

    public static void mockRoles(AltinnConfig altinnConfig, WireMockServer server, String altinnPath) {
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(altinnPath + "/authorization/roles"))
                .withHeader("X-NAV-APIKEY", equalTo(altinnConfig.getAPIGwHeader()))
                .withHeader("APIKEY", equalTo(altinnConfig.getAltinnHeader()))
                .withQueryParam("ForceEIAuthentication", equalTo(""))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(hentStringFraFil("roles.json"))
                ));
    }

    public static void mockAktorResponse(WireMockServer server, String aktorURL) {
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

    private void mockAadResponse(WireMockServer server, String aadPath) {
        server.stubFor(WireMock.post(WireMock.urlPathEqualTo(aadPath))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(hentStringFraFil("aadtoken.json"))
                ));
    }


    public static void mockSykemeldingerResponse(WireMockServer server, String sykemeldtePath) {
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(sykemeldtePath))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(hentStringFraFil("sykemeldinger.json"))
                ));
    }

    public static void mockSyfoOppgaverResponse(WireMockServer server, String syfoOppgavepath) {
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(syfoOppgavepath))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(hentStringFraFil("syfoOppgaver.json"))
                ));
    }
    public static void mockSyfoNarmesteLeder(WireMockServer server, String syfoNarmesteLederPath){
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo(syfoNarmesteLederPath))
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type","application/json")
                .withBody(hentStringFraFil("narmesteLeder.json"))
        ));

    }

    @SneakyThrows
    private static String hentStringFraFil(String filnavn) {
        return IOUtils.toString(MockServer.class.getClassLoader().getResourceAsStream("mock/" + filnavn), UTF_8);
    }
}
