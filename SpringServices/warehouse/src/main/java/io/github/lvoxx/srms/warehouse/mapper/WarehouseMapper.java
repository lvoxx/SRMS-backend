package io.github.lvoxx.srms.warehouse.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import io.github.lvoxx.srms.warehouse.dto.WarehouseDTO;
import io.github.lvoxx.srms.warehouse.models.Warehouse;
import io.github.lvoxx.srms.warehouse.models.WarehouseHistory;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface WarehouseMapper {

    // Convert Request DTO to Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastUpdatedBy", ignore = true)
    @Mapping(target = "isDeleted", constant = "false")
    Warehouse toEntity(WarehouseDTO.Request request);

    // Convert Entity to Response DTO
    @Mapping(target = "isBelowMinimum", expression = "java(warehouse.isBelowMinimum())")
    @Mapping(target = "isInStock", expression = "java(warehouse.isInStock())")
    WarehouseDTO.Response toResponse(Warehouse warehouse);

    // Update existing entity from UpdateRequest
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quantity", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastUpdatedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void updateEntityFromRequest(WarehouseDTO.UpdateRequest request, @MappingTarget Warehouse warehouse);

    // Convert InventoryTransactionRequest to WarehouseHistory
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    WarehouseHistory toHistoryEntity(WarehouseDTO.InventoryTransactionRequest request);

    // Convert WarehouseHistory to HistoryResponse
    @Mapping(target = "productName", ignore = true)
    WarehouseDTO.HistoryResponse toHistoryResponse(WarehouseHistory history);
}