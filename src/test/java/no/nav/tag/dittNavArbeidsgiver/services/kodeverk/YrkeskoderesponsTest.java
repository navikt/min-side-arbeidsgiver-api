package no.nav.tag.dittNavArbeidsgiver.services.kodeverk;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.Test;

public class YrkeskoderesponsTest {

    @Test
    @SneakyThrows
    public void yrkeskoderespons__skal_mappe_riktig_fra_json() {
        ObjectMapper objectMapper = new ObjectMapper();

        String json = "{" +
                "  \"betydninger\": {" +
                "    \"1227184\": [" +
                "      {" +
                "        \"gyldigFra\": \"2011-01-01\"," +
                "        \"gyldigTil\": \"9999-12-31\"," +
                "        \"beskrivelser\": {" +
                "          \"bm\": {" +
                "            \"term\": \"PLANSJEF (OFFENTLIG VIRKSOMHET)\"," +
                "            \"tekst\": \"PLANSJEF (OFFENTLIG VIRKSOMHET)\"" +
                "          }" +
                "        }" +
                "      }" +
                "    ]" +
                "  }" +
                "}";

        Yrkeskoderespons hei = objectMapper.readValue(json, Yrkeskoderespons.class);

        String jsonTilBeskrivelse = "{\n" +
                "          \"bm\": {\n" +
                "            \"term\": \"KONTORSJEF (OFFENTLIG VIRKSOMHET)\",\n" +
                "            \"tekst\": \"KONTORSJEF (OFFENTLIG VIRKSOMHET)\"\n" +
                "          }\n" +
                "        }";

        Beskrivelser beskrivelser = objectMapper.readValue(jsonTilBeskrivelse, Beskrivelser.class);

        String jsonTilSprak = "{\n" +
                "            \"term\": \"OPPSYNSSJEF (OFFENTLIG VIRKSOMHET)\",\n" +
                "            \"tekst\": \"OPPSYNSSJEF (OFFENTLIG VIRKSOMHET)\"\n" +
                "          }";

        Sprak sprak = objectMapper.readValue(jsonTilSprak, Sprak.class);

    }

}
