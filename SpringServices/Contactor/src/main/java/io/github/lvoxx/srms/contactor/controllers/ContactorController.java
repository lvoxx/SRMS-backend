package io.github.lvoxx.srms.contactor.controllers;

import java.util.UUID;

import org.springframework.hateoas.server.reactive.WebFluxLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.lvoxx.srms.common.dto.PageDTO;
import io.github.lvoxx.srms.contactor.dto.ContactorDTO;
import io.github.lvoxx.srms.contactor.hateos.ContactorResource;
import io.github.lvoxx.srms.contactor.services.ContactorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/contactors")
public class ContactorController {
    private final ContactorService contactorService;

    /**
     * Find contactor by ID
     * GET /contactors/{id}
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ContactorResource>> findById(@PathVariable UUID id) {
        return contactorService.findById(id)
                .flatMap(this::toResource)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Find all contactors with pagination
     * GET /contactors?p=0&s=10&sb=created_at&o=desc&del=false
     */
    @GetMapping("/")
    public Mono<PageDTO.Response<ContactorDTO.Response>> findAllPaged(
            @RequestParam(name = "p", required = false, defaultValue = "0") int page,
            @RequestParam(name = "s", required = false, defaultValue = "10") int size,
            @RequestParam(name = "sb", defaultValue = "created_at") String sortBy,
            @RequestParam(name = "o", defaultValue = "desc") String sortDirection,
            @RequestParam(name = "del", defaultValue = "false") boolean doWithDeleted) {

        // Create Request
        PageDTO.Request pageRequest = PageDTO.Request.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        return contactorService.findAllContactors(pageRequest, doWithDeleted);
    }

    /**
     * Create new contactor
     * POST /contactors
     */
    @PostMapping("/")
    public Mono<ResponseEntity<ContactorResource>> create(@Valid @RequestBody ContactorDTO.Request request) {
        return contactorService.create(request)
                .flatMap(this::toResource)
                .map(resource -> ResponseEntity.status(HttpStatus.CREATED).body(resource));
    }

    /**
     * Update contactor by ID
     * PUT /contactors/{id}
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<ContactorResource>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ContactorDTO.Request request) {
        return contactorService.updateContactor(id, request)
                .flatMap(this::toResource)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Soft delete contactor by ID
     * DELETE /contactors/{id}
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> softDelete(@PathVariable UUID id) {
        return contactorService.deleteContactor(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Restore soft-deleted contactor
     * PATCH /contactors/{id}/restore
     */
    @PatchMapping("/{id}/restore")
    public Mono<ResponseEntity<Void>> restore(@PathVariable UUID id) {
        return contactorService.restoreContactor(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    private Mono<ContactorResource> toResource(ContactorDTO.Response dto) {
        ContactorResource resource = new ContactorResource();
        resource.setId(dto.getId());
        resource.setContactorType(dto.getType().name());
        resource.setOrganizationName(dto.getOrganizationName());
        resource.setFullname(dto.getFullName());
        resource.setPhoneNumber(dto.getPhoneNumber());
        resource.setEmail(dto.getEmail());
        resource.setAddress(dto.getAddress());
        resource.setRating(dto.getRating().getRate());
        resource.setNotes(dto.getNotes());
        resource.setCreatedAt(dto.getCreatedAt());
        resource.setUpdatedAt(dto.getUpdatedAt());
        resource.setDeletedAt(dto.getDeletedAt());

        return WebFluxLinkBuilder.linkTo(
                WebFluxLinkBuilder.methodOn(ContactorController.class).findById(dto.getId()))
                .withSelfRel()
                .toMono()
                .map(selfLink -> {
                    resource.add(selfLink);
                    return resource;
                })
                .flatMap(res -> WebFluxLinkBuilder.linkTo(
                        WebFluxLinkBuilder.methodOn(ContactorController.class)
                                .findAllPaged(0, 10, "created_at", "desc", false))
                        .withRel("all-contactors")
                        .toMono()
                        .map(allContactorsLink -> {
                            res.add(allContactorsLink);
                            return res;
                        }));
    }
}
