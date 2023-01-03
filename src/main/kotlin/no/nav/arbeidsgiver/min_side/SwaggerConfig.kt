package no.nav.arbeidsgiver.min_side

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun springShopOpenAPI(): OpenAPI = OpenAPI().info(Info().title("Ditt Nav Arbeidsgiver API"))
}