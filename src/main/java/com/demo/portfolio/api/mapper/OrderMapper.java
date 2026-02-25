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
     * Maps {@link OrderEntity} to {@link Order} DTO.
     *
     * <p>The {@code customer} field is populated as a <em>partial</em> DTO containing
     * only the customer ID, extracted from the Hibernate proxy's embedded foreign-key
     * value without triggering a lazy-load round-trip. The remaining customer fields
     * ({@code firstName}, {@code lastName}, {@code email}) are resolved on demand by
     * the {@code Order.customer} DataLoader ({@code customerById}).</p>
     *
     * @param entity the order entity
     * @return the order DTO with a partial customer (id only)
     */
    @Mapping(target = "orderDate", expression = "java(entity.getOrderDate().toString())")
    @Mapping(target = "customer", expression = "java(partialCustomer(entity))")
    Order toDto(OrderEntity entity);

    /**
     * Builds a partial {@link com.demo.portfolio.api.generated.types.Customer} DTO
     * that carries only the customer ID. This is safe to call on a lazy-loaded entity
     * because Hibernate's proxy embeds the FK value and does not hit the database for
     * an {@code getId()} call.
     *
     * @param entity the order entity whose customer proxy will be inspected
     * @return a shallow {@link com.demo.portfolio.api.generated.types.Customer} with
     *         only {@code id} set
     */
    default com.demo.portfolio.api.generated.types.Customer partialCustomer(OrderEntity entity) {
        com.demo.portfolio.api.generated.types.Customer partial =
                new com.demo.portfolio.api.generated.types.Customer();
        partial.setId(String.valueOf(entity.getCustomer().getId()));
        return partial;
    }

    // Enum domain -> GraphQL enum (same values: MapStruct handles this automatically)
    com.demo.portfolio.api.generated.types.OrderStatus toDto(
        com.demo.portfolio.api.domain.OrderStatus status);

    // Optional: GraphQL enum -> domain enum
    com.demo.portfolio.api.domain.OrderStatus toEntity(
        com.demo.portfolio.api.generated.types.OrderStatus status);
}
