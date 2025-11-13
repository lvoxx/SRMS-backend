package io.github.lvoxx.srms.warehouse.hateoas;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * HATEOAS resource for warehouse health metrics responses.
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
public class WarehouseHealthMetricsResource extends RepresentationModel<WarehouseHealthMetricsResource> {
    private Long totalWarehouses;
    private Long belowMinimum;
    private Long outOfStock;
    private Double belowMinimumPercentage;
    private Double outOfStockPercentage;
    private Double healthyPercentage;
}
