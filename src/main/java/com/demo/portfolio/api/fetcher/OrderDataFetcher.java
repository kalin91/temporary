package com.demo.portfolio.api.fetcher;

import com.demo.portfolio.api.generated.DgsConstants;
import com.demo.portfolio.api.generated.types.*;
import com.demo.portfolio.api.mapper.CustomerMapper;
import com.demo.portfolio.api.mapper.OrderMapper;
import com.demo.portfolio.api.service.CustomerService;
import com.demo.portfolio.api.service.OrderService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
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
        private final CustomerService customerService;
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

        @DgsData(parentType = DgsConstants.ORDER.TYPE_NAME, field = DgsConstants.ORDER.Customer)
        public Mono<Customer> customerForOrder(DgsDataFetchingEnvironment dfe) {
                Order order = Objects.requireNonNull(dfe.getSource(), "Source for customerForOrder cannot be null");
                return orderService.getOrder(Long.parseLong(order.getId()))
                        .flatMap(orderEntity -> customerService.getCustomer(orderEntity.getCustomer().getId()))
                        .map(customerMapper::toDto);
        }
}
