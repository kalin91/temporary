package com.demo.portfolio.api.repository;

import com.demo.portfolio.api.domain.OrderEntity;
import com.demo.portfolio.api.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Order entities.
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    /**
     * Find orders by customer ID.
     *
     * @param customerId the customer ID
     * @param pageable pagination information
     * @return page of orders
     */
    Page<OrderEntity> findByCustomerId(Long customerId, Pageable pageable);
    /**
     * Find orders by status.
     *
     * @param status the order status
     * @param pageable pagination information
     * @return page of orders
     */
    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);
    /**
     * Find orders by customer ID and status.
     *
     * @param customerId the customer ID
     * @param status the order status
     * @param pageable pagination information
     * @return page of orders
     */
    Page<OrderEntity> findByCustomerIdAndStatus(Long customerId, OrderStatus status, Pageable pageable);
}
