package no.nav.security.spring.oidc;

import no.nav.security.spring.oidc.MultiIssuerProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@TestPropertySource(locations = {"/issuers.properties"})
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MultiIssuerProperties.class})
public class MultiIssuerConfigurationPropertiesTest {
    @Autowired
    private MultiIssuerProperties config;

    @Test
    public void test() {
        assertFalse(config.getIssuer().isEmpty());

        assertTrue(config.getIssuer().containsKey("number1"));
        assertEquals("http://metadata", config.getIssuer().get("number1").getDiscoveryUrl().toString());
        assertTrue(config.getIssuer().get("number1").getAcceptedAudience().contains("aud1"));
        assertEquals("idtoken", config.getIssuer().get("number1").getCookieName());

        assertTrue(config.getIssuer().containsKey("number2"));
        assertEquals("http://metadata2", config.getIssuer().get("number2").getDiscoveryUrl().toString());
        assertTrue(config.getIssuer().get("number2").getAcceptedAudience().contains("aud2"));
        assertEquals(null, config.getIssuer().get("number2").getCookieName());

        assertTrue(config.getIssuer().containsKey("number3"));
        assertEquals("http://metadata3", config.getIssuer().get("number3").getDiscoveryUrl().toString());
        System.out.println("config: " + config.getIssuer().get("number3").getAcceptedAudience());
        assertTrue(config.getIssuer().get("number3").getAcceptedAudience().contains("aud3")
                && config.getIssuer().get("number3").getAcceptedAudience().contains("aud4"));
    }

}
