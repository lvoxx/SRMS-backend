package io.github.lvoxx.srms.warehouse.hateoas;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * HATEOAS resource for warehouse statistics responses.
 * Extends RepresentationModel to support hypermedia links.
 * 
 * @author lvoxx
 * @version 1.0
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class WarehouseStatisticsResource extends RepresentationModel<WarehouseStatisticsResource> {
    private Long totalWarehouses;
    private Long inStock;
    private Long outOfStock;
    private Long belowMinimum;
    private Long totalHistoryEntries;
}
