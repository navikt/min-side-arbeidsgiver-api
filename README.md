#TAG - Ditt nav arbeidsgiver api
===========================
[![CircleCI](https://circleci.com/gh/navikt/ditt-nav-arbeidsgiver-api.svg?style=svg)](https://circleci.com/gh/navikt/ditt-nav-arbeidsgiver-api)

### Hensikt

Applikasjonen tilbyr et API som brukes fra ditt nav arbeidsgiver og potensielt andre arbeidsgiver-applikasjoner. Api-et leverer tilgjengelige bedrifter for en pålogget bruker og har noen endepunkter som integrerer mot Digisyfo for å kunne vise overordnet informasjon derfra.


### Oppsett

Applikasjonen bruker Spring boot 

Åpnes i browser: [http://localhost:8080/](http://localhost:8080/)

For lokal kjøring brukes Wiremock for mocking av andre integrasjoner.