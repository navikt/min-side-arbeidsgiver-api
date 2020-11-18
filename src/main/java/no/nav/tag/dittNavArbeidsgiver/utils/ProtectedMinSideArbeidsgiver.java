package no.nav.tag.dittNavArbeidsgiver.utils;

import no.nav.security.token.support.core.api.ProtectedWithClaims;

import javax.validation.Constraint;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ProtectedWithClaims(issuer="selvbetjening", claimMap={"acr=Level4"})
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Constraint(validatedBy = {})
public @interface ProtectedMinSideArbeidsgiver {
}
