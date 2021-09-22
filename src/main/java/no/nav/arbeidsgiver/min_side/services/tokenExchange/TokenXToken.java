package no.nav.arbeidsgiver.min_side.services.tokenExchange;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class TokenXToken {
    String access_token;
    String issued_token_type;
    String token_type;
    int expires_in;


}
