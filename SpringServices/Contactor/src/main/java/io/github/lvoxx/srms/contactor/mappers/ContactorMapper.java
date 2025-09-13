package io.github.lvoxx.srms.contactor.mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import io.github.lvoxx.srms.contactor.dto.ContactorDTO;
import io.github.lvoxx.srms.contactor.dto.Rating;
import io.github.lvoxx.srms.contactor.models.Contactor;
import io.github.lvoxx.srms.contactor.models.ContactorType;

@Mapper(componentModel = "spring")
public interface ContactorMapper {

    // ==================== Entity to DTO ====================
    
    @Mapping(target = "type", source = "contactorType", qualifiedByName = "stringToContactorType")
    @Mapping(target = "firstName", source = "fullname", qualifiedByName = "extractFirstName")
    @Mapping(target = "lastName", source = "fullname", qualifiedByName = "extractLastName")
    @Mapping(target = "attributes", source = "rating")
    ContactorDTO.Response toResponse(Contactor entity);

    List<ContactorDTO.Response> toResponseList(List<Contactor> entities);

    // ==================== DTO to Entity ====================
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "contactorType", source = "type", qualifiedByName = "contactorTypeToString")
    @Mapping(target = "rating", source = "rating", qualifiedByName = "ratingToString")
    Contactor toEntity(ContactorDTO.Request request);

    // ==================== Update Entity ====================
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "contactorType", source = "type", qualifiedByName = "contactorTypeToString")
    @Mapping(target = "rating", source = "rating", qualifiedByName = "ratingToString")
    void updateEntityFromRequest(ContactorDTO.Request request, @MappingTarget Contactor entity);

    // ==================== Custom Mapping Methods ====================

    @Named("stringToContactorType")
    default ContactorType stringToContactorType(String contactorType) {
        if (contactorType == null || contactorType.isEmpty()) {
            return null;
        }
        try {
            return ContactorType.valueOf(contactorType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ContactorType.OTHER;
        }
    }

    @Named("contactorTypeToString")
    default String contactorTypeToString(ContactorType type) {
        return type != null ? type.name() : null;
    }

    @Named("ratingToString")
    default String ratingToString(Rating rating) {
        return rating != null ? rating.getRate() : null;
    }

    @Named("extractFirstName")
    default String extractFirstName(String fullname) {
        if (fullname == null || fullname.trim().isEmpty()) {
            return null;
        }
        String[] parts = fullname.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : null;
    }

    @Named("extractLastName")
    default String extractLastName(String fullname) {
        if (fullname == null || fullname.trim().isEmpty()) {
            return null;
        }
        String[] parts = fullname.trim().split("\\s+");
        if (parts.length <= 1) {
            return null;
        }
        StringBuilder lastName = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (i > 1) lastName.append(" ");
            lastName.append(parts[i]);
        }
        return lastName.toString();
    }
}