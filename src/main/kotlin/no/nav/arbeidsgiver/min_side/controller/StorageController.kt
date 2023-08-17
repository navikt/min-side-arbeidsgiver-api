package no.nav.arbeidsgiver.min_side.controller

import no.nav.arbeidsgiver.min_side.services.storage.StaleStorageException
import no.nav.arbeidsgiver.min_side.services.storage.StorageEntry
import no.nav.arbeidsgiver.min_side.services.storage.StorageService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
    fun get(
        @PathVariable key: String
    ) = storageService.get(key, authenticatedUserHolder.fnr).asResponseEntity()

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
    ) = try {
        storageService.put(key, authenticatedUserHolder.fnr, value, version).asResponseEntity()
    } catch (e: StaleStorageException) {
        e.asResponseEntity()
    }

    @DeleteMapping(
        path = [
            "/api/storage/{key}",
            "/api/storage/{key}/",
        ]
    )
    fun delete(
        @PathVariable key: String,
        @RequestParam(required = false) version: Int?,
    ) = try {
        storageService.delete(key, authenticatedUserHolder.fnr, version).asResponseEntity()
    } catch (e: StaleStorageException) {
        e.asResponseEntity()
    }
}

fun StorageEntry?.asResponseEntity(): ResponseEntity<String> = if (this == null) {
    ResponseEntity.noContent().build()
} else {
    ResponseEntity
        .ok()
        .header("version", version.toString())
        .body(value)
}

fun StaleStorageException.asResponseEntity(): ResponseEntity<String> =
    ResponseEntity
        .status(HttpStatus.CONFLICT)
        .header("version", existing.version.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .body(existing.value)