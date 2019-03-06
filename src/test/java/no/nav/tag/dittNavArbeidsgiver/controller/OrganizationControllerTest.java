package no.nav.tag.dittNavArbeidsgiver.controller;

import org.junit.Test;

import static org.junit.Assert.*;

public class OrganizationControllerTest {
    @Test
    public static void test() {
        AltinnGW altinnGWMock = new AltinnGW();
        OrganizationController organizationController = new OrganizationController(altinnGWMock);
    }
}