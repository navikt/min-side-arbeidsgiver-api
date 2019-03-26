package no.nav.tag.dittNavArbeidsgiver.utils;

import lombok.Data;

@Data
public class AadAccessToken

{
    String access_token;
    String token_type;
    String expires_in;
    String ext_expires_in;
    String expires_on;
    String not_before;
    String resource;
}

