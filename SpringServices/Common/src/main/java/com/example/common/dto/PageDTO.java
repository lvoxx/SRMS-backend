package com.example.common.dto;

import java.util.List;

import org.springframework.lang.Nullable;

public abstract class PageDTO {

    public record PageRequestDTO(
            int page,
            int size,
            @Nullable String sortBy,
            @Nullable String sortDirection // "ASC" or "DESC"
    ) {
        public PageRequestDTO {
            page = page < 0 ? 0 : page; // Ensure non-negative page
            size = size <= 0 ? 10 : Math.min(size, 100); // Default size 10, max 100
            sortBy = sortBy != null ? sortBy : "created_at"; // Default sort by created_at
            sortDirection = sortDirection != null ? sortDirection.toUpperCase() : "DESC"; // Default DESC
        }
    }

    public record PageResponseDTO<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }
}
