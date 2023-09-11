package no.nav.arbeidsgiver.min_side

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.arbeidsgiver.min_side.models.Organisasjon
import no.nav.arbeidsgiver.min_side.services.altinn.AltinnService
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.core.io.Resource


@Import(
    LocalDittNavArbeidsgiverApplication.MockAltinnConfig::class,
)
class LocalDittNavArbeidsgiverApplication : DittNavArbeidsgiverApplication() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(LocalDittNavArbeidsgiverApplication::class.java, *args)
        }
    }

    class MockAltinnConfig {
        @Bean
        @Primary
        fun altinnService(
            objectMapper: ObjectMapper,
            @Value("classpath:mock/organisasjoner.json") organisasjonerJson: Resource,
            @Value("classpath:mock/rettigheterTilSkjema.json") rettigheterTilSkjemaJson: Resource,
        ): AltinnService {
            return mock(AltinnService::class.java).also {
                `when`(it.hentOrganisasjoner(anyString()))
                    .thenReturn(
                        objectMapper.readValue(
                            organisasjonerJson.inputStream.readAllBytes(),
                            Array<Organisasjon>::class.java
                        ).toList()
                    )
                `when`(
                    it.hentOrganisasjonerBasertPaRettigheter(
                        anyString(),
                        anyString(), //eq("4936"), wont work in kotlin https://derekwilson.net/blog/2018/08/23/mokito-kotlin
                        anyString(), //eq("1")
                    )
                ).thenReturn(
                    objectMapper.readValue(
                        rettigheterTilSkjemaJson.inputStream.readAllBytes(),
                        Array<Organisasjon>::class.java
                    ).toList()
                )
            }
        }
    }
}