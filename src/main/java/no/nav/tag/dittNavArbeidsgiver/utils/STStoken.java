package no.nav.tag.dittNavArbeidsgiver.utils;

import lombok.Data;

@Data
public class STStoken {
    String access_token;
    String token_type;
    int expires_in;
}
