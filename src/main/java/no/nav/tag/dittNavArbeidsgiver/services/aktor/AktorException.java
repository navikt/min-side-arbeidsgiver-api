package no.nav.tag.dittNavArbeidsgiver.services.aktor;

public class AktorException extends RuntimeException {
    AktorException(String msg, Exception e) {
        super(msg, e);
    }
}
