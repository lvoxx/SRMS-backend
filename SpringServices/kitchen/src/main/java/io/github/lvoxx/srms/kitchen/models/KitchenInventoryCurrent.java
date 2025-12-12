package io.github.lvoxx.srms.kitchen.models;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.github.lvoxx.srms.jdbc.AbstractPersonEntity;
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
public class KitchenInventoryCurrent extends AbstractPersonEntity {
    
    @Id
    @Column("id")
    private UUID id;

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

}