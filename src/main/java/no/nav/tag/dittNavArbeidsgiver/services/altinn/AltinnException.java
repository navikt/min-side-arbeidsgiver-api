package no.nav.tag.dittNavArbeidsgiver.services.altinn;

public class AltinnException extends RuntimeException {
    public AltinnException(String message) {
        super(message);
    }

    public AltinnException(String message, Exception e) {
        super(message, e);
    }
}
