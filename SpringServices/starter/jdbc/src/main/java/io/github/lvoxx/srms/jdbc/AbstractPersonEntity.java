package io.github.lvoxx.srms.jdbc;

import java.time.OffsetDateTime;

import org.springframework.data.relational.core.mapping.Column;

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
public abstract class AbstractPersonEntity {

    @Column("last_updated")
    @Default
    protected OffsetDateTime lastUpdated = OffsetDateTime.now();

    @Column("updated_by")
    protected String updatedBy;

}