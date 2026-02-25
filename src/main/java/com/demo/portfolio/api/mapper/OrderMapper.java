package com.demo.portfolio.api.mapper;

import com.demo.portfolio.api.domain.OrderEntity;
import com.demo.portfolio.api.generated.types.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for Order entity and DTO.
 */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    /**
     * Maps OrderEntity to Order DTO.
     *
     * @param entity the order entity
     * @return the order DTO
     */
    @Mapping(target = "orderDate", expression = "java(entity.getOrderDate().toString())")
    @Mapping(target = "customer", ignore = true) // Customer is fetched separately if needed
    Order toDto(OrderEntity entity);

    // Enum domain -> GraphQL enum (same values: MapStruct handles this automatically)
    com.demo.portfolio.api.generated.types.OrderStatus toDto(
        com.demo.portfolio.api.domain.OrderStatus status);

    // Optional: GraphQL enum -> domain enum
    com.demo.portfolio.api.domain.OrderStatus toEntity(
        com.demo.portfolio.api.generated.types.OrderStatus status);
}
