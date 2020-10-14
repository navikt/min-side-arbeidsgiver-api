package no.nav.tag.dittNavArbeidsgiver.mockserver;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static java.nio.charset.StandardCharsets.UTF_8;

@Profile({"local"})
@Slf4j
@Component
public class MockServer {

    public static final String SERVICE_EDITION = "1";
    public static final String SERVICE_CODE = "4936";
    public static final String ALTINN_PROXY_PATH ="/altinn-rettigheter-proxy/v2/organisasjoner/*";
    @SneakyThrows
    @Autowired
    MockServer(
            @Value("${mock.port}") int port,
            @Value("${sts.stsUrl}") String stsUrl,
            @Value("${aad.aadAccessTokenURL}") String aadUrl,
            @Value("${aktorregister.aktorUrl}") String aktorUrl,
            @Value("${digisyfo.sykemeldteURL}") String sykemeldteUrl,
            @Value("${digisyfo.syfooppgaveurl}") String syfoOpggaveUrl,
            @Value("${digisyfo.digisyfoUrl}") String digisyfoUrl
    ) {
        log.info("starter mockserveren");
        WireMockServer server = new WireMockServer(
                new WireMockConfiguration()
                        .port(port)
                        .extensions(new ResponseTemplateTransformer(true))
                        .notifier(new ConsoleNotifier(true))
        );
        String stsPath = new URL(stsUrl).getPath();
        String aadPath = new URL(aadUrl).getPath();
        String aktorPath = new URL(aktorUrl).getPath();
        String sykemeldtePath = new URL(sykemeldteUrl).getPath();
        String syfoOppgavePath = new URL(syfoOpggaveUrl).getPath();
        String syfoNarmesteLederPath = new URL(digisyfoUrl).getPath();

        mockForPath(server, ALTINN_PROXY_PATH, "organisasjoner.json");
        mockForPath(server, stsPath, "STStoken.json");
        mockForPath(server, aadPath, "aadtoken.json");
        mockForPath(server, aktorPath, "aktorer.json");
        mockForPath(server, sykemeldtePath, "sykemeldinger.json");
        mockForPath(server, syfoOppgavePath, "syfoOppgaver.json");
        mockForPath(server, syfoNarmesteLederPath, "narmesteLeder.json");
        mocktilgangTilSkjemForBedriftForAltinnProxy(server);
        server.start();
    }

    private static void mockForPath(WireMockServer server, String path, String responseFile){
        server.stubFor(WireMock.any(WireMock.urlPathMatching(path + ".*"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type","application/json")
                        .withBody(hentStringFraFil(responseFile))
                ));
    }
    private static void mocktilgangTilSkjemForBedriftForAltinnProxy(
            WireMockServer server
    ) {
        Map<String, StringValuePattern> parametre = new HashMap<>();
        parametre.put("serviceCode", equalTo(SERVICE_CODE));
        parametre.put("serviceEdition", equalTo(SERVICE_EDITION));

        server.stubFor(WireMock.get(WireMock.urlPathMatching(ALTINN_PROXY_PATH + ".*"))
                .withQueryParams(parametre)
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(hentStringFraFil("rettigheterTilSkjema.json"))
                ));
    }

    @SneakyThrows
    private static String hentStringFraFil(String filnavn) {
        return IOUtils.toString(MockServer.class.getClassLoader().getResourceAsStream("mock/" + filnavn), UTF_8);
    }
}