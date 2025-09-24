package io.github.lvoxx.srms.contactor.hateos;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.hateoas.RepresentationModel;

import io.github.lvoxx.srms.contactor.models.Contactor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ContactorResource extends RepresentationModel<ContactorResource> {
    private UUID id;
    private String contactorType;
    private String organizationName;
    private String fullname;
    private String phoneNumber;
    private String email;
    private String address;
    private String rating;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;

    public ContactorResource(Contactor contactor) {
        this.id = contactor.getId();
        this.contactorType = contactor.getContactorType();
        this.organizationName = contactor.getOrganizationName();
        this.fullname = contactor.getFullname();
        this.phoneNumber = contactor.getPhoneNumber();
        this.email = contactor.getEmail();
        this.address = contactor.getAddress();
        this.rating = contactor.getRating();
        this.notes = contactor.getNotes();
        this.createdAt = contactor.getCreatedAt();
        this.updatedAt = contactor.getUpdatedAt();
        this.deletedAt = contactor.getDeletedAt();
    }
}
