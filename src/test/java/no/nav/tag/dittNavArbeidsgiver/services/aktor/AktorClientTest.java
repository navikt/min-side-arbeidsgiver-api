package no.nav.tag.dittNavArbeidsgiver.services.aktor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
@TestPropertySource(properties = {"mock.port=8082"})
public class AktorClientTest {

    @Autowired
    private AktorClient aktorClient;

    @Test
    public void getAktorId() {
       String result = aktorClient.getAktorId("07045700172");
       assertEquals(result,"1000087745633");
    }
}