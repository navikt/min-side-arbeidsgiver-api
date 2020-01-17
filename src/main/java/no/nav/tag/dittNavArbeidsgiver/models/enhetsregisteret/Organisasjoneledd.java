package no.nav.tag.dittNavArbeidsgiver.models.enhetsregisteret;

import lombok.Data;

import java.util.ArrayList;

@Data
public class Organisasjoneledd {
    String organisasjonsnummer;
    ArrayList<OrganisasjonsleddOver> organisasjonsleddOver;
    ArrayList<InngaarIJuridiskEnheter> inngaarIJuridiskEnheter;
}
