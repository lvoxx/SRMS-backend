package io.github.lvoxx.srms.customer.models;

import java.time.OffsetDateTime;
import java.util.List;

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
@ToString(callSuper = true)
@Table("customer")
public class Customer extends AbstractEntity {

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("phone_number")
    private String phoneNumber;

    @Column("email")
    private String email;

    @Column("dietary_restrictions")
    private String[] dietaryRestrictions;

    @Column("allergies")
    private String[] allergies;

    @Column("is_regular")
    @Default
    private boolean isRegular = false;

    @Column("notes")
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

    // Helper methods for array conversion to List
    public List<String> getDietaryRestrictionsList() {
        return dietaryRestrictions != null ? List.of(dietaryRestrictions) : List.of();
    }

    public void setDietaryRestrictionsList(List<String> dietaryRestrictions) {
        this.dietaryRestrictions = dietaryRestrictions != null ? dietaryRestrictions.toArray(new String[0])
                : new String[0];
    }

    public List<String> getAllergiesList() {
        return allergies != null ? List.of(allergies) : List.of();
    }

    public void setAllergiesList(List<String> allergies) {
        this.allergies = allergies != null ? allergies.toArray(new String[0]) : new String[0];
    }
}