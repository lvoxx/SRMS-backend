package io.github.lvoxx.srms.customer.models;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.relational.core.mapping.Table;

import com.example.common.jdbc.AbstractAuditMapper;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "customer")
@SQLDelete(sql = "UPDATE customer SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Customer extends AbstractAuditMapper implements Serializable {

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 100)
    private String email;

    @ElementCollection
    @Column(name = "dietary_restrictions")
    private List<String> dietaryRestrictions;

    @ElementCollection
    @Column(name = "allergies")
    private List<String> allergies;

    @Column(name = "is_regular", nullable = false)
    private boolean isRegular;

    @Column(name = "notes")
    private String notes;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

}