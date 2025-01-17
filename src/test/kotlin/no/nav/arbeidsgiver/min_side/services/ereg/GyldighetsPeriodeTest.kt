package no.nav.arbeidsgiver.min_side.services.ereg

import no.nav.arbeidsgiver.min_side.services.ereg.GyldighetsPeriode.Companion.erGyldig
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GyldighetsPeriodeTest {

    @Test
    fun `fom er i fortiden, tom er null`() {
        assertTrue(GyldighetsPeriode(
            fom = LocalDate.parse("2024-01-01"),
            tom = null
        ).erGyldig())
    }
    @Test
    fun `fom er i fortiden, tom er i fremtiden`() {
        assertTrue(GyldighetsPeriode(
            fom = LocalDate.parse("2024-01-01"),
            tom = LocalDate.parse("5000-01-01"),
        ).erGyldig())
    }
    @Test
    fun `fom er i fortiden, tom er i fortiden`() {
        assertFalse(GyldighetsPeriode(
            fom = LocalDate.parse("2023-01-01"),
            tom = LocalDate.parse("2024-01-01"),
        ).erGyldig())
    }
    @Test
    fun `fom er null, tom er fremtiden`() {
        assertTrue(GyldighetsPeriode(
            fom = null,
            tom = LocalDate.parse("5000-01-01"),
        ).erGyldig())
    }
    @Test
    fun `fom er i fremtiden, tom er null`() {
        assertFalse(GyldighetsPeriode(
            fom = LocalDate.parse("5000-01-01"),
            tom = null,
        ).erGyldig())
    }
    @Test
    fun `fom er null, tom er fortiden`() {
        assertFalse(GyldighetsPeriode(
            fom = null,
            tom = LocalDate.parse("2024-01-01"),
        ).erGyldig())
    }
    @Test
    fun `gyldighetsperiode er null`() {
        assertTrue(null.erGyldig())
    }
}