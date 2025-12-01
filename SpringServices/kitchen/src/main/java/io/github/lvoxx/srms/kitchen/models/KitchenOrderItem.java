package io.github.lvoxx.srms.kitchen.models;

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
@Table("kitchen_order_items")
public class KitchenOrderItem extends AbstractEntity {

    @Column("kitchen_order_id")
    private UUID kitchenOrderId;

    @Column("menu_id")
    private UUID menuId;

    @Column("menu_name")
    private String menuName;

    @Column("quantity")
    private Integer quantity;

    @Column("unit")
    private String unit;

    @Column("special_request")
    private String specialRequest;

    @Column("item_status")
    private String itemStatus;
}