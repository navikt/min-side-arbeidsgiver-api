package no.nav.tag.dittNavArbeidsgiver.models.kodeverk;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.tag.dittNavArbeidsgiver.models.pdlBatch.PdlBatchRespons;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@Slf4j
 public class HentPersonBolkTest {
        @Test
        @SneakyThrows
        public void hentPerson__skal_mappe_riktig_fra_json() {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = "{\n" +
                    "    \"data\": {\n" +
                    "        \"hentPersonBolk\": [\n" +
                    "            {\n" +
                    "                \"ident\": \"13116224741\",\n" +
                    "                \"person\": {\n" +
                    "                    \"navn\": [\n" +
                    "                        {\n" +
                    "                            \"fornavn\": \"ABSURD\",\n" +
                    "                            \"etternavn\": \"BÆREPOSE\"\n" +
                    "                        }\n" +
                    "                    ]\n" +
                    "                },\n" +
                    "                \"code\": \"ok\"\n" +
                    "            },\n" +
                    "            {\n" +
                    "                \"ident\": \"17108025425\",\n" +
                    "                \"person\": {\n" +
                    "                    \"navn\": [\n" +
                    "                        {\n" +
                    "                            \"fornavn\": \"ABSURD\",\n" +
                    "                            \"etternavn\": \"GYNGEHEST\"\n" +
                    "                        }\n" +
                    "                    ]\n" +
                    "                },\n" +
                    "                \"code\": \"ok\"\n" +
                    "            }\n" +
                    "        ]\n" +
                    "    }\n" +
                    "}";

            PdlBatchRespons jsonTilSprak = objectMapper.readValue(json, PdlBatchRespons.class);
            assertEquals("13116224741", jsonTilSprak.data.hentPersonBolk[0].ident );
        }

    }
