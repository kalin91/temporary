package com.demo.portfolio.api.repository;

import com.demo.portfolio.api.domain.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for performing CRUD operations and queries on {@link com.demo.portfolio.api.domain.CustomerEntity}.
 * <p>
 * This interface extends {@link org.springframework.data.jpa.repository.JpaRepository}, providing standard methods for
 * persisting, retrieving, updating, and deleting customer records in the database. It is a part of the repository layer
 * in the application's layered architecture, abstracting the data access logic for the Customer domain.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Inject the repository in a service
 * @Autowired
 * private CustomerRepository customerRepository;
 *
 * // Find a customer by ID
 * Optional<CustomerEntity> customer = customerRepository.findById(1L);
 *
 * // Save a new customer
 * CustomerEntity saved = customerRepository.save(new CustomerEntity(...));
 * }</pre>
 *
 * <h2>Parameters</h2>
 * <ul>
 *   <li>{@code CustomerEntity}: The entity representing a customer record.</li>
 *   <li>{@code Long}: The type of the primary key (customer ID).</li>
 * </ul>
 *
 * <h2>Return Values</h2>
 * <ul>
 *   <li>Standard CRUD methods return {@code Optional<CustomerEntity>}, {@code List<CustomerEntity>}, or the entity itself depending on the operation.</li>
 * </ul>
 *
 * <h2>Exceptions</h2>
 * <ul>
 *   <li>Throws {@link org.springframework.dao.DataAccessException} for database access errors.</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This interface is thread-safe when used as a Spring bean, as Spring Data repositories are designed for concurrent access.
 * </p>
 */
@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
}
