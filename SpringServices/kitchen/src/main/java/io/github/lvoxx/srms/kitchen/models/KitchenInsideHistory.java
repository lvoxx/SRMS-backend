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
@Table("kitchen_inside_history")
public class KitchenInsideHistory extends AbstractEntity {

    @Column("transaction_type")
    private String transactionType;

    @Column("item_name")
    private String itemName;

    @Column("item_category")
    private String itemCategory;

    @Column("quantity")
    private BigDecimal quantity;

    @Column("unit")
    private String unit;

    @Column("source_reference")
    private String sourceReference;

    @Column("staff_name")
    private String staffName;

    @Column("transaction_date")
    private OffsetDateTime transactionDate;

    @Column("notes")
    private String notes;

    @Column("cost_amount")
    private BigDecimal costAmount;
}