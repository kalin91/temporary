package com.demo.portfolio.api.config;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.domain.OrderEntity;
import com.demo.portfolio.api.domain.OrderStatus;
import com.demo.portfolio.api.repository.CustomerRepository;
import com.demo.portfolio.api.repository.OrderRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Seeds the application's H2 database with sample customers and orders on
 * application startup.
 *
 * <p>This class reads sample data from JSON files located under
 * {@code src/main/resources/data/} and persists them using the
 * {@link com.demo.portfolio.api.repository.CustomerRepository} and
 * {@link com.demo.portfolio.api.repository.OrderRepository}.
 *
 * <p>Behavior:
 * <ul>
 *   <li>When the application starts and there are no customers in the DB,
 *   the seeder loads {@code customers.json} and {@code orders.json}.</li>
 *   <li>Customers are created first; orders are linked to customers by
 *   email. Orders referencing unknown emails are skipped with a warning.</li>
 *   <li>Any exception during seeding is logged and does not prevent
 *   application startup.</li>
 * </ul>
 *
 * <p>Note: This component is intended for development and demo purposes
 * (it uses H2 in-memory by default in this project). For production
 * seeding consider using dedicated migration tools like Flyway or Liquibase.
 *</p>
 *
 * @see com.demo.portfolio.api.repository.CustomerRepository
 * @see com.demo.portfolio.api.repository.OrderRepository
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    /**
     * Reads sample data files and persists their contents when the database
     * is empty.
     *
     * <p>The method performs the following steps:
     * <ol>
     *   <li>Load {@code data/customers.json} and map to {@link CustomerEntity}.</li>
     *   <li>Persist all customers.</li>
     *   <li>Load {@code data/orders.json} and map each entry to an
     *   {@link OrderEntity} linked to the corresponding customer by email.</li>
     *   <li>Persist all valid orders.</li>
     * </ol>
     *
     * @param args command line arguments passed to the application (ignored)
     */
    @Override
    public void run(String... args) {
        if (customerRepository.count() == 0) {
            log.info("Seeding database with sample data...");
            try {
                // Load Customers
                InputStream customerStream = new ClassPathResource("data/customers.json").getInputStream();
                List<CustomerData> customerDataList = objectMapper.readValue(customerStream, new TypeReference<>() {});
                
                List<CustomerEntity> customers = customerDataList.stream()
                        .map(c -> CustomerEntity.builder()
                                .firstName(c.getFirstName())
                                .lastName(c.getLastName())
                                .email(c.getEmail())
                                .build())
                        .collect(Collectors.toList());
                
                customerRepository.saveAll(customers);
                
                // Map email -> CustomerEntity for linking orders
                Map<String, CustomerEntity> customerMap = customers.stream()
                        .collect(Collectors.toMap(CustomerEntity::getEmail, c -> c));

                // Load Orders
                InputStream orderStream = new ClassPathResource("data/orders.json").getInputStream();
                List<OrderData> orderDataList = objectMapper.readValue(orderStream, new TypeReference<>() {});

                List<OrderEntity> orders = orderDataList.stream()
                        .map(o -> {
                            CustomerEntity customer = customerMap.get(o.getCustomerEmail());
                            if (customer == null) {
                                log.warn("Customer with email {} not found for order", o.getCustomerEmail());
                                return null;
                            }
                            return OrderEntity.builder()
                                    .customer(customer)
                                    .orderDate(OffsetDateTime.parse(o.getOrderDate()))
                                    .status(OrderStatus.valueOf(o.getStatus()))
                                    .totalAmount(o.getTotalAmount())
                                    .build();
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                orderRepository.saveAll(orders);

            } catch (Exception e) {
                log.error("Failed to seed database", e);
            }
            log.info("Database seeding completed.");
        }
    }
    
    /**
     * Internal DTO representing a customer entry in the seed JSON file.
     *
     * <p>Used only for deserialization and mapping to {@link CustomerEntity}.
     */
    @Data
    static class CustomerData {
        /** Given name of the customer. */
        private String firstName;
        /** Family name of the customer. */
        private String lastName;
        /** Unique email address used to identify and link orders. */
        private String email;
    }
    
    /**
     * Internal DTO representing an order entry in the seed JSON file.
     *
     * <p>Fields:
     * <ul>
     *   <li>{@code customerEmail} — email of the owning customer (used to
     *   link the order to an existing {@code CustomerEntity}).</li>
     *   <li>{@code orderDate} — ISO-8601 datetime string parsable by
     *   {@link java.time.OffsetDateTime#parse(CharSequence)}.</li>
     *   <li>{@code status} — one of the {@link OrderStatus} enum names.</li>
     *   <li>{@code totalAmount} — order total as a decimal value.</li>
     * </ul>
     */
    @Data
    static class OrderData {
        private String customerEmail;
        private String orderDate;
        private String status;
        private Double totalAmount;
    }
}
