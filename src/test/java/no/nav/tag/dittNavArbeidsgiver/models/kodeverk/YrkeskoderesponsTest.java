package no.nav.tag.dittNavArbeidsgiver.models.kodeverk;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import no.nav.tag.dittNavArbeidsgiver.models.Yrkeskoderespons.Beskrivelser;
import no.nav.tag.dittNavArbeidsgiver.models.Yrkeskoderespons.Sprak;
import no.nav.tag.dittNavArbeidsgiver.models.Yrkeskoderespons.Yrkeskoderespons;
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
                "          \"nb\": {" +
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
                "          \"nb\": {\n" +
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
