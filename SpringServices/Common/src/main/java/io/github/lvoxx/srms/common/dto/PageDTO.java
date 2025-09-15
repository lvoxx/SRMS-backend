package io.github.lvoxx.srms.common.dto;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import lombok.Builder;

public abstract class PageDTO {

    @Builder
    public record Request(
            int page,
            int size,
            @Nullable String sortBy,
            @Nullable String sortDirection // "ASC" or "DESC"
    ) {
        public Request {
            page = page < 0 ? 0 : page; // Ensure non-negative page
            size = size <= 0 ? 10 : Math.min(size, 100); // Default size 10, max 100
            sortBy = sortBy != null ? sortBy : "created_at"; // Default sort by created_at
            sortDirection = sortDirection != null ? sortDirection.toUpperCase() : "DESC"; // Default DESC
        }
    }

    @Builder
    public record Response<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    @SuppressWarnings("null")
    public static Pageable toPagable(@NonNull Request dto) {
        return PageRequest.of(
                dto.page(),
                dto.size(),
                Sort.by(dto.sortDirection().equalsIgnoreCase("ASC") ? Sort.Direction.ASC
                        : Sort.Direction.DESC,
                        dto.sortBy()));
    }
}
