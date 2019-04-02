package no.nav.tag.dittNavArbeidsgiver.services.aktor;

import lombok.Data;

import java.util.List;

@Data
public class Aktor {
    List <Ident> identer;
    String feilmelding;
}
