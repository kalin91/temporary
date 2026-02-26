package com.demo.portfolio.api.service;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.exception.ResourceNotFoundException;
import com.demo.portfolio.api.generated.types.CreateCustomerInput;
import com.demo.portfolio.api.generated.types.UpdateCustomerInput;
import com.demo.portfolio.api.repository.CustomerRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service layer for managing Customer domain operations.
 * <p>
 * This class encapsulates all business logic related to customers, including:
 * <ul>
 *   <li>Paginated retrieval of customers</li>
 *   <li>Single customer lookup by ID</li>
 *   <li>Creation, update, and deletion of customers</li>
 * </ul>
 * <p>
 * All methods are reactive and return Project Reactor {@link Mono} types, ensuring non-blocking execution.
 * <p>
 * The service enforces transactional boundaries and propagates {@link com.demo.portfolio.api.exception.ResourceNotFoundException}
 * when a requested customer does not exist. It is intended to be used by GraphQL data fetchers and other application layers.
 * <p>
 * <h2>Thread Safety</h2>
 * <p>
 * This service is stateless and thread-safe when used as a Spring bean.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * Retrieves a paginated list of customers.
     *
     * @param page the page number
     * @param size the page size
     * @return a Mono emitting the page of customers
     */
    @Transactional(readOnly = true)
    public Mono<Page<CustomerEntity>> getCustomers(int page, int size) {
        return executeBlocking("getCustomers", () -> customerRepository.findAll(PageRequest.of(page, size)));
    }

    /**
     * Retrieves a customer by ID.
     *
     * @param id the customer ID
     * @return a Mono emitting the customer
     */
    @Transactional(readOnly = true)
    public Mono<CustomerEntity> getCustomer(@NonNull Long id) {
        return executeBlocking("getCustomer", () -> customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id)))
            ;
    }

    /**
     * Creates a new customer.
     *
     * @param input the creation input
     * @return a Mono emitting the created customer
     */
    @Transactional
    public Mono<CustomerEntity> createCustomer(CreateCustomerInput input) {
        return executeBlocking("createCustomer", () -> {
            CustomerEntity entity = CustomerEntity.builder()
                .firstName(input.getFirstName())
                .lastName(input.getLastName())
                .email(input.getEmail())
                .build();
            return customerRepository.save(Objects.requireNonNull(entity));
        });
    }

    /**
     * Updates an existing customer.
     *
     * @param id the customer ID
     * @param input the update input
     * @return a Mono emitting the updated customer
     */
    @Transactional
    public Mono<CustomerEntity> updateCustomer(@NonNull Long id, UpdateCustomerInput input) {
        return executeBlocking("updateCustomer", () -> {
            CustomerEntity entity = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

            if (input.getFirstName() != null)
                entity.setFirstName(input.getFirstName());
            if (input.getLastName() != null)
                entity.setLastName(input.getLastName());
            if (input.getEmail() != null)
                entity.setEmail(input.getEmail());

            return customerRepository.save(Objects.requireNonNull(entity));
        });
    }

    /**
     * Deletes a customer.
     *
     * @param id the customer ID
     * @return a Mono emitting true if deleted
     */
    @Transactional
    public Mono<Boolean> deleteCustomer(@NonNull Long id) {
        return executeBlocking("deleteCustomer", () -> {
            if (!customerRepository.existsById(id)) {
                throw new ResourceNotFoundException("Customer not found with id: " + id);
            }
            customerRepository.deleteById(id);
            return true;
        });
    }

    /**
     * Executes a blocking callable on {@code boundedElastic} and applies
     * consistent service-level error logging.
     *
     * @param operation operation name for logs
     * @param action blocking action to execute
     * @param <T> result type
     * @return a {@link Mono} running on {@code boundedElastic}
     */
    private <T> Mono<T> executeBlocking(String operation, Callable<T> action) {
        return Mono.fromCallable(action)
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(ResourceNotFoundException.class,
                ex -> log.warn("CustomerService.{} not found: {}", operation, ex.getMessage()))
            .doOnError(ex -> {
                if (!(ex instanceof ResourceNotFoundException)) {
                    log.error("CustomerService.{} failed", operation, ex);
                }
            });
    }
}
