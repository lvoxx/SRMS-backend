package io.github.lvoxx.srms.warehouse.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.github.lvoxx.srms.jdbc.AbstractEntity;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
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
@ToString
@Table("warehouse")
public class Warehouse extends AbstractEntity{

    @Column("product_name")
    private String productName;

    @Column("quantity")
    @Default
    private Integer quantity = 0;

    @Column("min_quantity")
    @Default
    private Integer minQuantity = 0;

    @Column("contactor_id")
    private UUID contactorId;

    @Column("last_updated_by")
    private String lastUpdatedBy;

    @Column("is_deleted")
    @Default
    private Boolean isDeleted = false;
    
    // Soft delete method
    public void markAsDeleted() {
        this.isDeleted = true;
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isDeleted() {
        return this.isDeleted != null && this.isDeleted;
    }

    // Check if quantity is below minimum threshold
    public boolean isBelowMinimum() {
        return this.quantity != null && this.minQuantity != null 
            && this.quantity < this.minQuantity;
    }

    // Check if product is in stock
    public boolean isInStock() {
        return this.quantity != null && this.quantity > 0;
    }
}