package io.github.lvoxx.srms.warehouse.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

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
@Table("warehouse_history")
public class WarehouseHistory {

    @Id
    @Column("id")
    private UUID id;

    @Column("warehouse_id")
    private UUID warehouseId;

    @Column("quantity")
    private Integer quantity;

    @Column("type")
    private String type;

    @Column("updated_by")
    private String updatedBy;

    @Column("created_at")
    @Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public enum HistoryType {
        IMPORT("import"),
        EXPORT("export");

        private final String value;

        HistoryType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static HistoryType fromValue(String value) {
            for (HistoryType type : HistoryType.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid history type: " + value);
        }
    }
}