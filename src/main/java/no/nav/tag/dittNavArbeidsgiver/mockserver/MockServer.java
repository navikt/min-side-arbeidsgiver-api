package no.nav.tag.dittNavArbeidsgiver.mockserver;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URL;

import static java.nio.charset.StandardCharsets.UTF_8;

@Profile({"dev"})
@Slf4j
@Component
public class MockServer {

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


        mockForPath(server, stsPath, "STStoken.json");
        mockForPath(server, aadPath, "aadtoken.json");
        mockForPath(server, aktorPath, "aktorer.json");
        mockForPath(server, sykemeldtePath, "sykemeldinger.json");
        mockForPath(server, syfoOppgavePath, "syfoOppgaver.json");
        mockForPath(server, syfoNarmesteLederPath, "narmesteLeder.json");
        server.start();
    }

    private static void mockForPath(WireMockServer server, String path, String responseFile){
        server.stubFor(WireMock.any(WireMock.urlPathMatching(path + ".*"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type","application/json")
                        .withBody(hentStringFraFil(responseFile))
                ));
    }

    @SneakyThrows
    private static String hentStringFraFil(String filnavn) {
        return IOUtils.toString(MockServer.class.getClassLoader().getResourceAsStream("mock/" + filnavn), UTF_8);
    }
}