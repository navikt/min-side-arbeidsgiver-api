package no.nav.tag.dittNavArbeidsgiver.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Right {
    @JsonProperty("ServiceCode")
    private int serviceCode;
    @JsonProperty("RightType")
    private String rightType;
    @JsonProperty("RightID")
    private int rightID;
    @JsonProperty("Action")
    private String actionType;
    @JsonProperty("ServiceEditionCode")
    private int serviceEditionCode;
    @JsonProperty("IsDelegatable")
    private boolean isDelegatable;
    @JsonProperty("RightSourceType")
    private String rightSourceType;


}








