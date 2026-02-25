package com.demo.portfolio.api.service;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.domain.OrderEntity;
import com.demo.portfolio.api.domain.OrderStatus;
import com.demo.portfolio.api.exception.ResourceNotFoundException;
import com.demo.portfolio.api.generated.types.CreateOrderInput;
import com.demo.portfolio.api.generated.types.UpdateOrderInput;
import com.demo.portfolio.api.repository.CustomerRepository;
import com.demo.portfolio.api.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;

/**
 * Service class for managing order operations.
 * 
 * This service provides CRUD operations and query functionality for orders, including:
 * - Retrieving orders with optional filtering by customer ID and/or status
 * - Creating new orders with customer validation
 * - Updating existing orders
 * - Deleting orders
 * 
 * All operations are performed asynchronously using Project Reactor's Mono and are
 * executed on bounded elastic schedulers to avoid blocking the main thread.
 * 
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    /**
     * Retrieves a paginated list of orders, optionally filtered by customer ID and/or status.
     *
     * @param customerId the optional customer ID
     * @param status the order status to filter by
     * @param page the page number
     * @param size the page size
     * @return a Mono emitting the page of orders
     */
    @Transactional(readOnly = true)
    public Mono<Page<OrderEntity>> getOrders(Long customerId, OrderStatus status, int page, int size) {
        return Mono.fromCallable(() -> {
            boolean hasCustomerId = customerId != null;
            boolean hasStatus = status != null;
            PageRequest pageRequest = PageRequest.of(page, size);
            if (hasCustomerId && hasStatus)
                return orderRepository.findByCustomerIdAndStatus(customerId, status, pageRequest);
            else if (hasStatus)
                return orderRepository.findByStatus(status, pageRequest);
            else if (hasCustomerId)
                return orderRepository.findByCustomerId(customerId, pageRequest);
            return orderRepository.findAll(pageRequest);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Retrieves an order by ID.
     *
     * @param id the order ID
     * @return a Mono emitting the order
     */
    @Transactional(readOnly = true)
    public Mono<OrderEntity> getOrder(Long id) {
        return Mono.fromCallable(() -> orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Creates a new order.
     *
     * @param input the creation input
     * @return a Mono emitting the created order
     */
    @Transactional
    public Mono<OrderEntity> createOrder(CreateOrderInput input) {
        return Mono.fromCallable(() -> {
            Long customerId = Long.parseLong(input.getCustomerId());
            CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

            OrderEntity entity = OrderEntity.builder()
                .customer(customer)
                .orderDate(OffsetDateTime.now())
                .status(OrderStatus.PENDING)
                .totalAmount(input.getTotalAmount())
                .build();
            return orderRepository.save(entity);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Updates an existing order.
     *
     * @param id the order ID
     * @param input the update input
     * @return a Mono emitting the updated order
     */
    @Transactional
    public Mono<OrderEntity> updateOrder(Long id, UpdateOrderInput input) {
        return Mono.fromCallable(() -> {
            OrderEntity entity = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
            if (input.getStatus() != null)
                entity.setStatus(OrderStatus.valueOf(input.getStatus().name()));
            if (input.getTotalAmount() != null)
                entity.setTotalAmount(input.getTotalAmount());
            return orderRepository.save(entity);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes an order.
     *
     * @param id the order ID
     * @return a Mono emitting true if deleted
     */
    @Transactional
    public Mono<Boolean> deleteOrder(Long id) {
        return Mono.fromCallable(() -> {
            if (!orderRepository.existsById(id))
                throw new ResourceNotFoundException("Order not found with id: " + id);
            orderRepository.deleteById(id);
            return true;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
