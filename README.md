#TAG - Ditt nav arbeidsgiver api
===========================
[![CircleCI](https://circleci.com/gh/navikt/ditt-nav-arbeidsgiver-api.svg?style=svg)](https://circleci.com/gh/navikt/ditt-nav-arbeidsgiver-api)

Applikasjonen tilbyr et API som brukes fra ditt nav arbeidsgiver og potensielt andre arbeidsgiver-applikasjoner. Api-et leverer tilgjengelige bedrifter for en pålogget bruker og har noen endepunkter som integrerer mot Digisyfo for å kunne vise overordnet informasjon derfra.


### Komme i gang

Applikasjonen bruker Spring boot. Ved lokal kjøring brukes Wiremock for mocking av integrasjoner.

* Swagger finner du her: http://localhost:8080/ditt-nav-arbeidsgiver-api/swagger-ui.html#/
* For å teste endpunktene lokalt kan man hente autorisasjons cookie fra http://localhost:8080/ditt-nav-arbeidsgiver-api/local/cookie
med cookiename: selvbetjening-idtoken

## Henvendelser
Spørsmål knyttet til koden eller prosjektet kan rettes mot:

* Bendik Segrov Ibenholt, bendik.segrov.ibenholt@nav.no
* Hilde Steinbru Heggstad, hilde.steinbru.heggstad@nav.no
* Torstein Gjengedal, torstein.gjengedal@nav.no

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #tag-min-side-arbeidsgiver
