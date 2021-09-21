package no.nav.tag.dittNavArbeidsgiver.exceptions;

public class TilgangskontrollException extends RuntimeException {

    public TilgangskontrollException(String message) {
        super(message);
    }

    public TilgangskontrollException(String message, Throwable cause) {
        super(message, cause);
    }
}
