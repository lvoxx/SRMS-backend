package io.github.lvoxx.srms.customer.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import io.github.lvoxx.srms.customer.dto.CustomerDTO;
import io.github.lvoxx.srms.customer.models.Customer;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CustomerMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "dietaryRestrictions", target = "dietaryRestrictions")
    @Mapping(source = "allergies", target = "allergies")
    @Mapping(source = "isRegular", target = "isRegular")
    @Mapping(source = "notes", target = "notes")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(source = "deletedAt", target = "deletedAt")
    CustomerDTO.Response toResponse(Customer customer);

    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "dietaryRestrictions", target = "dietaryRestrictions")
    @Mapping(source = "allergies", target = "allergies")
    @Mapping(source = "isRegular", target = "isRegular")
    @Mapping(source = "notes", target = "notes")
    Customer toCustomer(CustomerDTO.Request request);

    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "dietaryRestrictions", target = "dietaryRestrictions")
    @Mapping(source = "allergies", target = "allergies")
    @Mapping(source = "isRegular", target = "isRegular")
    @Mapping(source = "notes", target = "notes")
    void updateCustomerFromRequest(CustomerDTO.Request request, @MappingTarget Customer customer);
}
