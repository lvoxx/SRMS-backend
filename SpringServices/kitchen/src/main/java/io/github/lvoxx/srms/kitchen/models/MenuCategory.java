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
import lombok.Builder.Default;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@Table("menu_category")
public class MenuCategory extends AbstractEntity {

    @Column("ctg_parent_id")
    private UUID ctgParentId;

    @Column("category_name")
    private String categoryName;

    @Column("display_order")
    @Default
    private Integer displayOrder = 0;

    @Column("is_active")
    @Default
    private Boolean isActive = true;
}