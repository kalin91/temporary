package com.demo.portfolio.api.repository;

import com.demo.portfolio.api.domain.OrderEntity;
import com.demo.portfolio.api.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository interface for performing CRUD operations and custom queries on {@link com.demo.portfolio.api.domain.OrderEntity}.
 * <p>
 * This interface extends {@link org.springframework.data.jpa.repository.JpaRepository}, providing standard methods for persisting,
 * retrieving, updating, and deleting order records in the database, as well as filtering orders by customer, status, or both. It is a
 * part of the repository layer in the application's layered architecture, abstracting the data access logic for the Order domain.
 * </p>
 *
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * // Inject the repository in a service
 * &#64;Autowired
 * private OrderRepository orderRepository;
 *
 * // Find orders by customer ID
 * Page<OrderEntity> orders = orderRepository.findByCustomerId(1L, PageRequest.of(0, 10));
 *
 * // Find orders by status
 * Page<OrderEntity> shipped = orderRepository.findByStatus(OrderStatus.SHIPPED, PageRequest.of(0, 10));
 *
 * // Save a new order
 * OrderEntity saved = orderRepository.save(new OrderEntity(...));
 * }</pre>
 *
 * <h2>Parameters</h2>
 * <ul>
 * <li>{@code OrderEntity}: The entity representing an order record.</li>
 * <li>{@code Long}: The type of the primary key (order ID).</li>
 * <li>{@code OrderStatus}: Enum representing the status of an order.</li>
 * <li>{@code Pageable}: Pagination information for queries.</li>
 * </ul>
 *
 * <h2>Return Values</h2>
 * <ul>
 * <li>Standard CRUD methods return {@code Optional<OrderEntity>}, {@code List<OrderEntity>}, or the entity itself depending on the
 * operation.</li>
 * <li>Custom query methods return {@code Page<OrderEntity>} for paginated results.</li>
 * </ul>
 *
 * <h2>Exceptions</h2>
 * <ul>
 * <li>Throws {@link org.springframework.dao.DataAccessException} for database access errors.</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This interface is thread-safe when used as a Spring bean, as Spring Data repositories are designed for concurrent access.
 * </p>
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

    /**
     * Batch-fetch all orders belonging to a set of customer IDs in a single query.
     * Used by the DataLoader to eliminate N+1 queries when resolving {@code Customer.orders}.
     *
     * @param customerIds the set of customer IDs to query for
     * @return flat list of all matching orders (unsorted)
     */
    List<OrderEntity> findByCustomerIdIn(Collection<Long> customerIds);

    /**
     * Finds all orders for a collection of customers with optional status filtering.
     * 
     * @param customerIds Collection of customer IDs to filter orders by. Must not be null or empty.
     * @param status The order status to filter by. If null, all statuses are included.
     * @param pageable The pagination and sorting information. Must not be null.
     * @return A list of OrderEntity objects matching the specified criteria, or an empty list if no matches found.
     */
    @Query("SELECT o FROM OrderEntity o WHERE o.customer.id IN :customerIds AND (:status IS NULL OR o.status = :status)")
    List<OrderEntity> findByCustomerIdInAndOptionalStatus(Collection<Long> customerIds, OrderStatus status, Pageable pageable);

}
