package com.demo.portfolio.api.service;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.exception.ResourceNotFoundException;
import com.demo.portfolio.api.generated.types.CreateCustomerInput;
import com.demo.portfolio.api.generated.types.UpdateCustomerInput;
import com.demo.portfolio.api.repository.CustomerRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service for managing customers.
 */
@Service
@RequiredArgsConstructor
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
        return Mono.fromCallable(() -> customerRepository.findAll(PageRequest.of(page, size)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Retrieves a customer by ID.
     *
     * @param id the customer ID
     * @return a Mono emitting the customer
     */
    @Transactional(readOnly = true)
    public Mono<CustomerEntity> getCustomer(@NonNull Long id) {
        return Mono.fromCallable(() -> customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Creates a new customer.
     *
     * @param input the creation input
     * @return a Mono emitting the created customer
     */
    @Transactional
    public Mono<CustomerEntity> createCustomer(CreateCustomerInput input) {
        return Mono.fromCallable(() -> {
            CustomerEntity entity = CustomerEntity.builder()
                .firstName(input.getFirstName())
                .lastName(input.getLastName())
                .email(input.getEmail())
                .build();
            return customerRepository.save(Objects.requireNonNull(entity));
        }).subscribeOn(Schedulers.boundedElastic());
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
        return Mono.fromCallable(() -> {
            CustomerEntity entity = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

            if (input.getFirstName() != null)
                entity.setFirstName(input.getFirstName());
            if (input.getLastName() != null)
                entity.setLastName(input.getLastName());
            if (input.getEmail() != null)
                entity.setEmail(input.getEmail());

            return customerRepository.save(Objects.requireNonNull(entity));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes a customer.
     *
     * @param id the customer ID
     * @return a Mono emitting true if deleted
     */
    @Transactional
    public Mono<Boolean> deleteCustomer(@NonNull Long id) {
        return Mono.fromCallable(() -> {
            if (!customerRepository.existsById(id)) {
                throw new ResourceNotFoundException("Customer not found with id: " + id);
            }
            customerRepository.deleteById(id);
            return true;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
