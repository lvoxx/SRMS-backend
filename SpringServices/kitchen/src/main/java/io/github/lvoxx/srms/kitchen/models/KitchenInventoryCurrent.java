package io.github.lvoxx.srms.kitchen.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.github.lvoxx.srms.jdbc.AbstractEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@Table("kitchen_inventory_current")
public class KitchenInventoryCurrent extends AbstractEntity {
    
    @Column("item_name")
    private String itemName;
    
    @Column("item_category")
    private String itemCategory;
    
    @Column("current_quantity")
    private BigDecimal currentQuantity;
    
    @Column("unit")
    private String unit;
    
    @Column("min_threshold")
    private BigDecimal minThreshold;
    
    @Column("max_threshold")
    private BigDecimal maxThreshold;
    
    @Column("last_updated")
    private OffsetDateTime lastUpdated;
    
    @Column("updated_by")
    private String updatedBy;
}