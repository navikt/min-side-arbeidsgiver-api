package no.nav.tag.dittNavArbeidsgiver.services.sts;

import lombok.Data;

@Data
class STStoken {
    String access_token;
    String token_type;
    int expires_in;
}
