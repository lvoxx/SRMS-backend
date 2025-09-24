package io.github.lvoxx.srms.common.jdbc;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

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
@ToString
public abstract class AbstractEntity {
    @Id
    @Column("id")
    private UUID id;

    @Column("created_at")
    @Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column("updated_at")
    private OffsetDateTime updatedAt;

}
