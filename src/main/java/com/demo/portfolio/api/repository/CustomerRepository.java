package com.demo.portfolio.api.repository;

import com.demo.portfolio.api.domain.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Customer entities.
 */
@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
}
