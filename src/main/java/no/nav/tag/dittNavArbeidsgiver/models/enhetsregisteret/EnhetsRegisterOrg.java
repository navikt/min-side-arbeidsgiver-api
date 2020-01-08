package no.nav.tag.dittNavArbeidsgiver.models.enhetsregisteret;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.nav.tag.dittNavArbeidsgiver.models.enhetsregisteret.BestaarAvOrganisasjonsledd;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class EnhetsRegisterOrg {
    @JsonProperty("organisasjonsnummer")
    private String organisasjonsnummer;
    Map<String, Object> orgTre = new LinkedHashMap<>();
    ArrayList<BestaarAvOrganisasjonsledd> bestaarAvOrganisasjonsledd = new ArrayList <BestaarAvOrganisasjonsledd> ();

}

