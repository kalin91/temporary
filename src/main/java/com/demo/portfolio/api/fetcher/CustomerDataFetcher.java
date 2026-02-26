package com.demo.portfolio.api.fetcher;

import com.demo.portfolio.api.config.Permission;
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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * GraphQL Data Fetcher for Customer operations.
 * <p>
 * This component defines the GraphQL queries and mutations for the Customer domain, including:
 * <ul>
 * <li>Paginated and filtered retrieval of customers</li>
 * <li>Single customer lookup by ID</li>
 * <li>Creation, update, and deletion of customers</li>
 * <li>Resolution of the {@code orders} field for each customer using a batched DataLoader</li>
 * </ul>
 * <p>
 * All methods return Project Reactor {@link Mono} types for reactive, non-blocking execution.
 * <p>
 * This class is a Spring bean annotated with {@link DgsComponent} and is automatically discovered by the DGS framework.
 */
@DgsComponent
@RequiredArgsConstructor
public class CustomerDataFetcher {

        private final CustomerService customerService;
        private final CustomerMapper customerMapper;


        /**
         * Retrieves a paginated list of customers.
         *
         * @param page the zero-based page index (optional, defaults to 0)
         * @param size the maximum number of customers per page (optional, defaults to 10)
         * @return a {@link Mono} emitting a {@link CustomerPage} containing the paginated customers and page info
         */
        @DgsQuery
        public Mono<CustomerPage> customers(@InputArgument Integer page, @InputArgument Integer size) {
                int p = page != null ? page : 0;
                int s = size != null ? size : 10;

                return customerService.getCustomers(p, s)
                        .map(customerPage -> {
                                List<Customer> customers = customerPage.getContent().stream()
                                        .map(customerMapper::toDto)
                                        .toList();

                                return CustomerPage.newBuilder()
                                        .content(customers)
                                        .totalElements((int) customerPage.getTotalElements())
                                        .totalPages(customerPage.getTotalPages())
                                        .number(customerPage.getNumber())
                                        .size(customerPage.getSize())
                                        .numberOfElements(customerPage.getNumberOfElements())
                                        .first(customerPage.isFirst())
                                        .last(customerPage.isLast())
                                        .empty(customerPage.isEmpty())
                                        .build();
                        });
        }


        /**
         * Retrieves a single customer by its unique identifier.
         *
         * @param id the customer ID as a string
         * @return a {@link Mono} emitting the {@link Customer} DTO, or an error if not found
         */
        @DgsQuery
        public Mono<Customer> customer(@InputArgument String id) {
                return customerService.getCustomer(Long.parseLong(id))
                        .map(customerMapper::toDto);
        }


        /**
         * Creates a new customer from the provided input.
         *
         * @param input the input data for the new customer
         * @return a {@link Mono} emitting the created {@link Customer} DTO
         */
        @DgsMutation
        @PreAuthorize(Permission.ROLE_WRITER)
        public Mono<Customer> createCustomer(@Valid @InputArgument CreateCustomerInput input) {
                return customerService.createCustomer(input)
                        .map(customerMapper::toDto);
        }


        /**
         * Updates an existing customer with the provided input data.
         *
         * @param id the customer ID as a string
         * @param input the update data for the customer
         * @return a {@link Mono} emitting the updated {@link Customer} DTO
         */
        @DgsMutation
        @PreAuthorize(Permission.ROLE_WRITER)
        public Mono<Customer> updateCustomer(@InputArgument String id, @InputArgument UpdateCustomerInput input) {
                return customerService.updateCustomer(Long.parseLong(id), input)
                        .map(customerMapper::toDto);
        }


        /**
         * Deletes a customer by its unique identifier. Deleting a customer will automatically cascade-delete all associated orders
         *
         * @param id the customer ID as a string
         * @return a {@link Mono} emitting {@code true} if the customer was deleted, or {@code false} otherwise
         */
        @DgsMutation
        @PreAuthorize(Permission.ROLE_ADMIN)
        public Mono<Boolean> deleteCustomer(@InputArgument String id) {
                return customerService.deleteCustomer(Long.parseLong(id));
        }

        /**
         * Resolves the {@code Customer.orders} field using a batched DataLoader, eliminating
         * the N+1 problem when a list of customers is returned with their nested orders.
         *
         * <p>
         * The customer ID, optional status filter, and pagination parameters are packed into
         * a composite {@link OrdersByCustomerKey} and forwarded to {@link OrdersByCustomerDataLoader}.
         * All keys accumulated during a single GraphQL execution tick are dispatched in one batch;
         * filtering and pagination are pushed down to the database query inside the loader,
         * so no in-memory post-processing is performed here.
         * </p>
         *
         * @param dfe the DGS data-fetching environment providing the source {@link Customer}
         * @param page zero-based page index for the orders result set
         * @param size maximum number of orders to return per page
         * @param statusFilter optional {@link OrderStatus} to filter orders; {@code null} returns all statuses
         * @return a {@link Mono} emitting the filtered and paginated list of {@link Order} DTOs
         */
        @DgsData(parentType = DgsConstants.CUSTOMER.TYPE_NAME, field = DgsConstants.CUSTOMER.Orders)
        public Mono<List<Order>> ordersForCustomer(DgsDataFetchingEnvironment dfe, @InputArgument("page") int page,
                @InputArgument("size") int size, @InputArgument("status") OrderStatus statusFilter) {
                Customer customer = Objects.requireNonNull(dfe.getSource(), "Source for ordersForCustomer cannot be null");
                Long customerId = Long.parseLong(customer.getId());
                DataLoader<OrdersByCustomerKey, List<Order>> loader = dfe.getDataLoader(OrdersByCustomerDataLoader.class);
                PageRequest pageRequest = PageRequest.of(page, size);
                OrdersByCustomerKey key = new OrdersByCustomerKey(customerId, statusFilter, pageRequest);
                return Mono.fromFuture(loader.load(key));
        }
}
