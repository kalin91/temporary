package com.demo.portfolio.api.fetcher;

import com.demo.portfolio.api.generated.types.*;
import com.demo.portfolio.api.mapper.CustomerMapper;
import com.demo.portfolio.api.mapper.OrderMapper;
import com.demo.portfolio.api.service.CustomerService;
import com.demo.portfolio.api.service.OrderService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GraphQL Data Fetcher for Customer operations.
 */
@DgsComponent
@RequiredArgsConstructor
public class CustomerDataFetcher {

    private final CustomerService customerService;
    private final OrderService orderService;
    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;

    @DgsQuery
    public Mono<CustomerConnection> customers(@InputArgument Integer page, @InputArgument Integer size) {
        int p = page != null ? page : 0;
        int s = size != null ? size : 10;
        
        return customerService.getCustomers(p, s)
                .map(customerPage -> {
                    List<CustomerEdge> edges = customerPage.getContent().stream()
                            .map(entity -> CustomerEdge.newBuilder()
                                    .cursor(String.valueOf(entity.getId()))
                                    .node(customerMapper.toDto(entity))
                                    .build())
                            .collect(Collectors.toList());
                    
                    PageInfo pageInfo = PageInfo.newBuilder()
                            .hasNextPage(customerPage.hasNext())
                            .hasPreviousPage(customerPage.hasPrevious())
                            .build();
                    
                    return CustomerConnection.newBuilder()
                            .edges(edges)
                            .pageInfo(pageInfo)
                            .build();
                });
    }

    @DgsQuery
    public Mono<Customer> customer(@InputArgument String id) {
        return customerService.getCustomer(Long.parseLong(id))
                .map(customerMapper::toDto);
    }

    @DgsMutation
    public Mono<Customer> createCustomer(@InputArgument CreateCustomerInput input) {
        return customerService.createCustomer(input)
                .map(customerMapper::toDto);
    }

    @DgsMutation
    public Mono<Customer> updateCustomer(@InputArgument String id, @InputArgument UpdateCustomerInput input) {
        return customerService.updateCustomer(Long.parseLong(id), input)
                .map(customerMapper::toDto);
    }

    @DgsMutation
    public Mono<Boolean> deleteCustomer(@InputArgument String id) {
        return customerService.deleteCustomer(Long.parseLong(id));
    }

    @DgsData(parentType = "Customer", field = "orders")
    public Mono<List<Order>> ordersForCustomer(com.netflix.graphql.dgs.DgsDataFetchingEnvironment dfe) {
        Customer customer = dfe.getSource();
        return orderService.getOrders(Long.parseLong(customer.getId()), 0, 100)
                .map(orderPage -> orderPage.getContent().stream()
                        .map(orderMapper::toDto)
                        .collect(Collectors.toList()));
    }
}
