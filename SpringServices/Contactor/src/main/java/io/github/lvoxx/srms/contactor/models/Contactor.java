package io.github.lvoxx.srms.contactor.models;

import java.time.OffsetDateTime;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.github.lvoxx.srms.common.jdbc.AbstractEntity;
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
@ToString
@Table("contactor")
public class Contactor extends AbstractEntity {

    @Column("contact_type")
    private ContactorType type;

    @Column("organization_name")
    private String organizationName;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("phone_number")
    private String phoneNumber;

    private String email;

    private String address; // JSONB → có thể để String hoặc Map<String,Object>
    private String attributes; // JSONB → tương tự

    private String notes;

    @Column("deleted_at")
    private OffsetDateTime deletedAt;

    // Soft delete method
    public void markAsDeleted() {
        this.deletedAt = OffsetDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}