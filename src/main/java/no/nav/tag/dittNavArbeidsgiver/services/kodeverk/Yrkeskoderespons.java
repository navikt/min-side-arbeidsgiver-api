package no.nav.tag.dittNavArbeidsgiver.services.kodeverk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Yrkeskoderespons {
    private Map<String, List<Yrkeskode>> betydninger;

    public Map<String, List<Yrkeskode>> getBetydninger() {
        return betydninger;
    }


    public void setBetydninger(Map<String, List<Yrkeskode>> betydninger) {
        this.betydninger = new LinkedHashMap<>(betydninger);
    }
}
