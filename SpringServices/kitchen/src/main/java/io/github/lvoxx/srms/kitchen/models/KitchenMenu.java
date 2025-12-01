package io.github.lvoxx.srms.kitchen.models;

import java.math.BigDecimal;
import java.util.UUID;

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
@Table("kitchen_menu")
public class KitchenMenu extends AbstractEntity {
    
    @Column("menu_name")
    private String menuName;
    
    @Column("image_url")
    private String imageUrl;
    
    @Column("min_quantity")
    private Integer minQuantity;
    
    @Column("unit")
    private String unit;
    
    @Column("max_quantity")
    private Integer maxQuantity;
    
    @Column("menu_ctg_id")
    private UUID menuCtgId;
    
    @Column("price")
    private BigDecimal price;
    
    @Column("description")
    private String description;
    
    @Column("is_available")
    private Boolean isAvailable;
    
    @Column("preparation_time_minutes")
    private Integer preparationTimeMinutes;
}