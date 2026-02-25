package com.demo.portfolio.api.fetcher;

import com.demo.portfolio.api.dto.OrdersByCustomerKey;
import com.demo.portfolio.api.generated.DgsConstants;
import com.demo.portfolio.api.generated.types.*;
import com.demo.portfolio.api.mapper.CustomerMapper;
import com.demo.portfolio.api.service.CustomerService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.data.domain.PageRequest;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * GraphQL Data Fetcher for Customer operations.
 */
@DgsComponent
@RequiredArgsConstructor
public class CustomerDataFetcher {

        private final CustomerService customerService;
        private final CustomerMapper customerMapper;

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
                                        .toList();

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

        /**
         * Resolves the {@code Customer.orders} field using a batched DataLoader, eliminating
         * the N+1 problem when a list of customers is returned with their nested orders.
         *
         * <p>The customer ID, optional status filter, and pagination parameters are packed into
         * a composite {@link OrdersByCustomerKey} and forwarded to {@link OrdersByCustomerDataLoader}.
         * All keys accumulated during a single GraphQL execution tick are dispatched in one batch;
         * filtering and pagination are pushed down to the database query inside the loader,
         * so no in-memory post-processing is performed here.</p>
         *
         * @param dfe          the DGS data-fetching environment providing the source {@link Customer}
         * @param page         zero-based page index for the orders result set
         * @param size         maximum number of orders to return per page
         * @param statusFilter optional {@link OrderStatus} to filter orders; {@code null} returns all statuses
         * @return a {@link Mono} emitting the filtered and paginated list of {@link Order} DTOs
         */
        @DgsData(parentType = DgsConstants.CUSTOMER.TYPE_NAME, field = DgsConstants.CUSTOMER.Orders)
        public Mono<List<Order>> ordersForCustomer(DgsDataFetchingEnvironment dfe, @InputArgument("page") int page, @InputArgument("size") int size, @InputArgument("status") OrderStatus statusFilter) {
                Customer customer = Objects.requireNonNull(dfe.getSource(), "Source for ordersForCustomer cannot be null");
                Long customerId = Long.parseLong(customer.getId());
                DataLoader<OrdersByCustomerKey, List<Order>> loader = dfe.getDataLoader(OrdersByCustomerDataLoader.class);
                PageRequest pageRequest = PageRequest.of(page, size);
                OrdersByCustomerKey key = new OrdersByCustomerKey(customerId, statusFilter, pageRequest);
                return Mono.fromFuture(loader.load(key));
        }
}
