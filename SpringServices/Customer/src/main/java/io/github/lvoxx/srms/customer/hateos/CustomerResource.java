package io.github.lvoxx.srms.customer.hateos;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.hateoas.RepresentationModel;

import io.github.lvoxx.srms.customer.models.Customer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CustomerResource extends RepresentationModel<CustomerResource> {
    private UUID id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private List<String> dietaryRestrictions;
    private List<String> allergies;
    private boolean isRegular;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;

    public CustomerResource(Customer customer) {
        this.id = customer.getId();
        this.firstName = customer.getFirstName();
        this.lastName = customer.getLastName();
        this.phoneNumber = customer.getPhoneNumber();
        this.email = customer.getEmail();
        this.dietaryRestrictions = customer.getDietaryRestrictionsList();
        this.allergies = customer.getAllergiesList();
        this.isRegular = customer.isRegular();
        this.notes = customer.getNotes();
        this.createdAt = customer.getCreatedAt();
        this.updatedAt = customer.getUpdatedAt();
        this.deletedAt = customer.getDeletedAt();
    }
}
