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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * GraphQL Data Fetcher for Order operations.
 */
@DgsComponent
@RequiredArgsConstructor
public class OrderDataFetcher {

        private final OrderService orderService;
        private final OrderMapper orderMapper;
        private final CustomerMapper customerMapper;

        @DgsQuery
        public Mono<OrderConnection> orders(@InputArgument @Nullable String customerId, @InputArgument @Nullable OrderStatus status,
                @InputArgument Integer page, @InputArgument Integer size) {
                int p = page != null ? page : 0;
                int s = size != null ? size : 10;
                Long cId = customerId != null ? Long.parseLong(customerId) : null;

                return orderService.getOrders(cId, orderMapper.toEntity(status), p, s)
                        .map(orderPage -> {
                                List<OrderEdge> edges = orderPage.getContent().stream()
                                        .map(entity -> OrderEdge.newBuilder()
                                                .cursor(String.valueOf(entity.getId()))
                                                .node(orderMapper.toDto(entity))
                                                .build())
                                        .toList();

                                PageInfo pageInfo = PageInfo.newBuilder()
                                        .hasNextPage(orderPage.hasNext())
                                        .hasPreviousPage(orderPage.hasPrevious())
                                        .build();

                                return OrderConnection.newBuilder()
                                        .edges(edges)
                                        .pageInfo(pageInfo)
                                        .build();
                        });
        }

        @DgsQuery
        public Mono<Order> order(@InputArgument String id) {
                return orderService.getOrder(Long.parseLong(id))
                        .map(orderMapper::toDto);
        }

        @DgsMutation
        public Mono<Order> createOrder(@InputArgument CreateOrderInput input) {
                return orderService.createOrder(input)
                        .map(orderMapper::toDto);
        }

        @DgsMutation
        public Mono<Order> updateOrder(@InputArgument String id, @InputArgument UpdateOrderInput input) {
                return orderService.updateOrder(Long.parseLong(id), input)
                        .map(orderMapper::toDto);
        }

        @DgsMutation
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
