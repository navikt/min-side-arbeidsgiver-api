package no.nav.arbeidsgiver.min_side

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class DittNavArbeidsgiverApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(DittNavArbeidsgiverApplication::class.java, *args)
        }
    }
}