package com.demo.portfolio.api.mapper;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.generated.types.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for Customer entity and DTO.
 */
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    /**
     * Maps CustomerEntity to Customer DTO.
     *
     * @param entity the customer entity
     * @return the customer DTO
     */
    @Mapping(target = "orders", ignore = true) // Orders are fetched separately via data fetcher
    Customer toDto(CustomerEntity entity);
}
