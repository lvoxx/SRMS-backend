package io.github.lvoxx.srms.customer.controllers;

import java.util.List;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.lvoxx.srms.common.dto.PageDTO;
import io.github.lvoxx.srms.customer.dto.CustomerDTO;
import io.github.lvoxx.srms.customer.hateos.CustomerResource;
import io.github.lvoxx.srms.customer.services.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/customers")
public class CustomerController {
        private final CustomerService customerService;

        @GetMapping("/{id}")
        public Mono<ResponseEntity<CustomerResource>> findById(@PathVariable UUID id) {
                return customerService.findById(id)
                                .flatMap(dto -> toResource(dto)
                                                .map(ResponseEntity::ok))
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }

        @GetMapping
        public Mono<PageDTO.Response<CustomerDTO.Response>> findAllPaged(
                        @RequestParam(name = "p", required = false, defaultValue = "0") int page, //
                        @RequestParam(name = "s", required = false, defaultValue = "10") int size,
                        @RequestParam(name = "sb", defaultValue = "created_by") String sortBy,
                        @RequestParam(name = "o", defaultValue = "desc") String sortDirection,
                        @RequestParam(name = "del", defaultValue = "false") boolean doWithDeleted) {
                return customerService.findAllPaged(new PageDTO.Request(page, size, sortBy, sortDirection),
                                doWithDeleted);
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public Mono<ResponseEntity<CustomerResource>> create(@Valid @RequestBody CustomerDTO.Request request) {
                return customerService.create(request)
                                .flatMap(dto -> toResource(dto)
                                                .map(resource -> ResponseEntity.created(
                                                                resource.getRequiredLink("self").toUri())
                                                                .body(resource)));
        }

        @PutMapping("/{id}")
        public Mono<ResponseEntity<CustomerResource>> update(@PathVariable UUID id,
                        @Valid @RequestBody CustomerDTO.Request request) {
                return customerService.update(id, request)
                                .flatMap(dto -> toResource(dto))
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }

        @DeleteMapping("/{id}")
        public Mono<ResponseEntity<Void>> softDelete(@PathVariable UUID id) {
                return customerService.softDelete(id)
                                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }

        @PatchMapping("/{id}/restore")
        public Mono<ResponseEntity<Void>> restore(@PathVariable UUID id) {
                return customerService.restore(id)
                                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                                .defaultIfEmpty(ResponseEntity.notFound().build());
        }

        private Mono<CustomerResource> toResource(CustomerDTO.Response dto) {
                CustomerResource resource = new CustomerResource();
                resource.setId(dto.getId());
                resource.setFirstName(dto.getFirstName());
                resource.setLastName(dto.getLastName());
                resource.setPhoneNumber(dto.getPhoneNumber());
                resource.setEmail(dto.getEmail());
                resource.setDietaryRestrictions(List.of(dto.getDietaryRestrictions()));
                resource.setAllergies(List.of(dto.getAllergies()));
                resource.setRegular(dto.isRegular());
                resource.setNotes(dto.getNotes());
                resource.setCreatedAt(dto.getCreatedAt());
                resource.setUpdatedAt(dto.getUpdatedAt());
                resource.setDeletedAt(dto.getDeletedAt());

                return WebFluxLinkBuilder.linkTo(
                                WebFluxLinkBuilder.methodOn(CustomerController.class).findById(dto.getId()))
                                .withSelfRel()
                                .toMono()
                                .map(selfLink -> {
                                        resource.add(selfLink);
                                        return resource;
                                })
                                .flatMap(res -> WebFluxLinkBuilder.linkTo(
                                                WebFluxLinkBuilder.methodOn(CustomerController.class)
                                                                .findAllPaged(0, 10, "createdAt", "desc", false))
                                                .withRel("all-customers")
                                                .toMono()
                                                .map(allCustomersLink -> {
                                                        res.add(allCustomersLink);
                                                        return res;
                                                }));
        }
}