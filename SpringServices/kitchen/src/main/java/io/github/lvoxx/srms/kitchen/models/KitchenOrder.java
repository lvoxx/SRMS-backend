package io.github.lvoxx.srms.kitchen.models;

import java.time.OffsetDateTime;
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
@Table("kitchen_orders")
public class KitchenOrder extends AbstractEntity {
    
    @Column("order_id")
    private UUID orderId;
    
    @Column("table_location")
    private String tableLocation;
    
    @Column("staff_name")
    private String staffName;
    
    @Column("order_time")
    private OffsetDateTime orderTime;
    
    @Column("status")
    private String status;
    
    @Column("kitchen_note")
    private String kitchenNote;
    
    @Column("customer_note")
    private String customerNote;
    
    @Column("completed_at")
    private OffsetDateTime completedAt;
}