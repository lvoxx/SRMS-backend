package io.github.lvoxx.srms.contactor.models;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.github.lvoxx.srms.common.jdbc.AbstractEntity;
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
@ToString(callSuper = true)
@Table("contactor")
public class Contactor extends AbstractEntity {

    @Column("contactor_type")
    private String contactorType;

    @Column("organization_name")
    private String organizationName;

    @Column("fullname")
    private String fullname;

    @Column("phone_number")
    private String phoneNumber;

    private String email;

    @Default
    private Map<String, Object> address = new HashMap<>();

    @Default
    private Map<String, Object> attributes = new HashMap<>();
    private String notes;

    @Column("deleted_at")
    @Default
    private OffsetDateTime deletedAt = null;

    // Soft delete method
    public void markAsDeleted() {
        this.deletedAt = OffsetDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

}