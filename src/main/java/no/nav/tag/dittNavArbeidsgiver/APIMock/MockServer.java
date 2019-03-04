package no.nav.tag.dittNavArbeidsgiver.APIMock;

import org.mockserver.integration.ClientAndServer;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

public class MockServer {

    private ClientAndServer mockServer;

       public void startServer() {
        mockServer = startClientAndServer(1080);
    }

       public void stopServer() {
        mockServer.stop();
    }

}
