package com.demo.portfolio.api.service;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.generated.types.CreateCustomerInput;
import com.demo.portfolio.api.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createCustomer_ShouldReturnCreatedCustomer() {
        CreateCustomerInput input = new CreateCustomerInput("John", "Doe", "john@example.com");
        CustomerEntity savedEntity = new CustomerEntity(1L, "John", "Doe", "john@example.com", null);

        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(savedEntity);

        Mono<CustomerEntity> result = customerService.createCustomer(input);

        StepVerifier.create(result)
                .expectNextMatches(entity -> entity.getId().equals(1L) && entity.getFirstName().equals("John"))
                .verifyComplete();
    }
}
