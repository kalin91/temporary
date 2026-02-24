package com.demo.portfolio.api.config;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.domain.OrderEntity;
import com.demo.portfolio.api.domain.OrderStatus;
import com.demo.portfolio.api.repository.CustomerRepository;
import com.demo.portfolio.api.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Seeds the database with sample data on startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.count() == 0) {
            log.info("Seeding database with sample data...");
            
            CustomerEntity customer1 = CustomerEntity.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .build();
                    
            CustomerEntity customer2 = CustomerEntity.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .build();
                    
            customerRepository.saveAll(List.of(customer1, customer2));
            
            OrderEntity order1 = OrderEntity.builder()
                    .customer(customer1)
                    .orderDate(OffsetDateTime.now().minusDays(2))
                    .status(OrderStatus.DELIVERED)
                    .totalAmount(150.50)
                    .build();
                    
            OrderEntity order2 = OrderEntity.builder()
                    .customer(customer1)
                    .orderDate(OffsetDateTime.now())
                    .status(OrderStatus.PROCESSING)
                    .totalAmount(99.99)
                    .build();
                    
            OrderEntity order3 = OrderEntity.builder()
                    .customer(customer2)
                    .orderDate(OffsetDateTime.now().minusDays(5))
                    .status(OrderStatus.SHIPPED)
                    .totalAmount(250.00)
                    .build();
                    
            orderRepository.saveAll(List.of(order1, order2, order3));
            
            log.info("Database seeding completed.");
        }
    }
}
