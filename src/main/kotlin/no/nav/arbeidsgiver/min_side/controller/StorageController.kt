package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.services.storage.StorageService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ProtectedWithClaims(
    issuer = AuthenticatedUserHolder.TOKENX,
    claimMap = [AuthenticatedUserHolder.REQUIRED_LOGIN_LEVEL]
)
@RestController
class StorageController(
    val storageService: StorageService,
    val authenticatedUserHolder: AuthenticatedUserHolder,
) {
    @GetMapping(
        path = [
            "/api/storage/{key}",
            "/api/storage/{key}/",
        ]
    )
    fun get(@PathVariable key: String): ResponseEntity<String> {
        val entry = storageService.get(key, authenticatedUserHolder.fnr)
        return if (entry == null) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity
                .ok()
                .header("version", entry.version.toString())
                .body(entry.value)
        }
    }

    @PutMapping(
        path = [
            "/api/storage/{key}",
            "/api/storage/{key}/",
        ]
    )
    fun put(
        @PathVariable key: String,
        @RequestParam(required = false) version: Int?,
        @RequestBody value: String
    ) = storageService.put(key, authenticatedUserHolder.fnr, value, version)

    @DeleteMapping(
        path = [
            "/api/storage/{key}",
            "/api/storage/{key}/",
        ]
    )
    fun delete(
        @PathVariable key: String,
        @RequestParam(required = false) version: Int?,
    ) = storageService.delete(key, authenticatedUserHolder.fnr, version)
}