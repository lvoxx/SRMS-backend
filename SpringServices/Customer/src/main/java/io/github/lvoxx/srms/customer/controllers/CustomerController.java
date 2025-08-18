package io.github.lvoxx.srms.customer.controllers;

import java.util.UUID;

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

import com.example.common.dto.PageDTO;

import io.github.lvoxx.srms.customer.dto.CustomerDTO;
import io.github.lvoxx.srms.customer.services.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RequestMapping("/customers")
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping("/{id}")
    public Mono<ResponseEntity<CustomerDTO.Response>> findById(@PathVariable UUID id) {
        return customerService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Mono<PageDTO.PageResponseDTO<CustomerDTO.Response>> findAllPaged(
            @RequestParam(name = "p", required = false) int page, //
            @RequestParam(name = "s", required = false) int size,
            @RequestParam(name = "sb", defaultValue = "created_by") String sortBy,
            @RequestParam(name = "o", defaultValue = "desc") String sortDirection,
            @RequestParam(name = "del", defaultValue = "false") boolean doWithDeleted) {
        return customerService.findAllPaged(new PageDTO.PageRequestDTO(page, size, sortBy, sortDirection),
                doWithDeleted);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CustomerDTO.Response> create(@Valid @RequestBody CustomerDTO.Request request) {
        return customerService.create(request);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<CustomerDTO.Response>> update(@PathVariable UUID id,
            @Valid @RequestBody CustomerDTO.Request request) {
        return customerService.update(id, request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> softDelete(@PathVariable UUID id) {
        return customerService.softDelete(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @PatchMapping("/{id}/restore")
    public Mono<ResponseEntity<CustomerDTO.Response>> restore(@PathVariable UUID id) {
        return customerService.restore(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
