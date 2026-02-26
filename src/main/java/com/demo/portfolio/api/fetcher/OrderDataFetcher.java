package com.demo.portfolio.api.fetcher;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.generated.DgsConstants;
import com.demo.portfolio.api.generated.types.*;
import com.demo.portfolio.api.mapper.CustomerMapper;
import com.demo.portfolio.api.mapper.OrderMapper;
import com.demo.portfolio.api.service.OrderService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.security.access.prepost.PreAuthorize;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * GraphQL Data Fetcher for Order operations.
 * <p>
 * This component defines the GraphQL queries and mutations for the Order domain, including:
 * <ul>
 *   <li>Paginated and filtered retrieval of orders</li>
 *   <li>Single order lookup by ID</li>
 *   <li>Creation, update, and deletion of orders</li>
 *   <li>Resolution of the {@code customer} field for each order using a batched DataLoader</li>
 * </ul>
 * <p>
 * All methods return Project Reactor {@link Mono} types for reactive, non-blocking execution.
 * <p>
 * This class is a Spring bean annotated with {@link DgsComponent} and is automatically discovered by the DGS framework.
 */
@DgsComponent
@RequiredArgsConstructor
public class OrderDataFetcher {

        private final OrderService orderService;
        private final OrderMapper orderMapper;
        private final CustomerMapper customerMapper;


        /**
         * Retrieves a paginated list of orders, optionally filtered by customer ID and/or status.
         *
         * @param customerId the customer ID as a string (optional, may be {@code null})
         * @param status     the order status to filter by (optional, may be {@code null})
         * @param page       the zero-based page index (optional, defaults to 0)
         * @param size       the maximum number of orders per page (optional, defaults to 10)
         * @return a {@link Mono} emitting an {@link OrderPage} containing the paginated orders and page info
         */
        @DgsQuery
        public Mono<OrderPage> orders(@InputArgument @Nullable String customerId, @InputArgument @Nullable OrderStatus status,
                @InputArgument Integer page, @InputArgument Integer size) {
                int p = page != null ? page : 0;
                int s = size != null ? size : 10;
                Long cId = customerId != null ? Long.parseLong(customerId) : null;

                return orderService.getOrders(cId, orderMapper.toEntity(status), p, s)
                        .map(orderPage -> {
                                List<Order> orders = orderPage.getContent().stream()
                                        .map(orderMapper::toDto)
                                        .toList();

                                return OrderPage.newBuilder()
                                        .content(orders)
                                        .totalElements((int) orderPage.getTotalElements())
                                        .totalPages(orderPage.getTotalPages())
                                        .number(orderPage.getNumber())
                                        .size(orderPage.getSize())
                                        .numberOfElements(orderPage.getNumberOfElements())
                                        .first(orderPage.isFirst())
                                        .last(orderPage.isLast())
                                        .empty(orderPage.isEmpty())
                                        .build();
                        });
        }


        /**
         * Retrieves a single order by its unique identifier.
         *
         * @param id the order ID as a string
         * @return a {@link Mono} emitting the {@link Order} DTO, or an error if not found
         */
        @DgsQuery
        public Mono<Order> order(@InputArgument String id) {
                return orderService.getOrder(Long.parseLong(id))
                        .map(orderMapper::toDto);
        }


        /**
         * Creates a new order from the provided input.
         *
         * @param input the input data for the new order
         * @return a {@link Mono} emitting the created {@link Order} DTO
         */
        @DgsMutation
        @PreAuthorize("hasRole('WRITER')")
        public Mono<Order> createOrder(@InputArgument CreateOrderInput input) {
                return orderService.createOrder(input)
                        .map(orderMapper::toDto);
        }


        /**
         * Updates an existing order with the provided input data.
         *
         * @param id    the order ID as a string
         * @param input the update data for the order
         * @return a {@link Mono} emitting the updated {@link Order} DTO
         */
        @DgsMutation
        @PreAuthorize("hasRole('WRITER')")
        public Mono<Order> updateOrder(@InputArgument String id, @InputArgument UpdateOrderInput input) {
                return orderService.updateOrder(Long.parseLong(id), input)
                        .map(orderMapper::toDto);
        }


        /**
         * Deletes an order by its unique identifier.
         *
         * @param id the order ID as a string
         * @return a {@link Mono} emitting {@code true} if the order was deleted, or {@code false} otherwise
         */
        @DgsMutation
        @PreAuthorize("hasRole('ADMIN')")
        public Mono<Boolean> deleteOrder(@InputArgument String id) {
                return orderService.deleteOrder(Long.parseLong(id));
        }

        /**
         * Resolves the {@code Order.customer} field using a batched DataLoader, eliminating
         * the N+1 problem when a list of orders is returned with their nested customer.
         *
         * <p>The customer ID is read from the partial {@link Customer} DTO that was populated
         * by {@link com.demo.portfolio.api.mapper.OrderMapper#partialCustomer} during entity
         * mapping — this access does <em>not</em> trigger a Hibernate lazy-load because the
         * mapper reads only the FK value from the proxy.</p>
         *
         * <p>All customer IDs accumulated during a single GraphQL tick are forwarded to
         * {@link CustomerByIdDataLoader} in one batch query.</p>
         *
         * @param dfe the DGS data-fetching environment providing the source order
         * @return a {@link Mono} emitting the fully-populated {@link Customer} DTO
         */
        @DgsData(parentType = DgsConstants.ORDER.TYPE_NAME, field = DgsConstants.ORDER.Customer)
        public Mono<Customer> customerForOrder(DgsDataFetchingEnvironment dfe) {
                Order order = Objects.requireNonNull(dfe.getSource(), "Source for customerForOrder cannot be null");
                // customer.id is set by the OrderMapper partial mapping — no DB hit here
                Long customerId = Long.parseLong(order.getCustomer().getId());
                DataLoader<Long, CustomerEntity> loader = dfe.getDataLoader(CustomerByIdDataLoader.class);
                return Mono.fromFuture(loader.load(customerId))
                        .map(customerMapper::toDto);
        }
}
