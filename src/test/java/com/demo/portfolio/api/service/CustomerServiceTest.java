package com.demo.portfolio.api.service;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.exception.ResourceNotFoundException;
import com.demo.portfolio.api.generated.types.CreateCustomerInput;
import com.demo.portfolio.api.generated.types.UpdateCustomerInput;
import com.demo.portfolio.api.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void getCustomersReturnsPage() {
        Page<CustomerEntity> page = new PageImpl<>(List.of(CustomerEntity.builder().id(1L).build()));
        when(customerRepository.findAll(PageRequest.of(0, 10))).thenReturn(page);

        Page<CustomerEntity> result = customerService.getCustomers(0, 10).block();

        assertSame(page, result);
    }

    @Test
    void getCustomerReturnsEntityWhenFound() {
        CustomerEntity entity = CustomerEntity.builder().id(1L).build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(entity));

        CustomerEntity result = customerService.getCustomer(1L).block();

        assertSame(entity, result);
    }

    @Test
    void getCustomerThrowsWhenMissing() {
        when(customerRepository.findById(9L)).thenReturn(Optional.empty());

        Mono<CustomerEntity> result = customerService.getCustomer(9L);
        assertThrows(ResourceNotFoundException.class, result::block);
    }

    @Test
    void createCustomerSavesEntity() {
        CreateCustomerInput input = mock(CreateCustomerInput.class);
        when(input.getFirstName()).thenReturn("John");
        when(input.getLastName()).thenReturn("Doe");
        when(input.getEmail()).thenReturn("john@acme.com");

        CustomerEntity saved = CustomerEntity.builder().id(1L).firstName("John").build();
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(saved);

        CustomerEntity result = customerService.createCustomer(input).block();

        assertSame(saved, result);
        verify(customerRepository).save(any(CustomerEntity.class));
    }

    @Test
    void updateCustomerUpdatesNonNullFields() {
        CustomerEntity existing = CustomerEntity.builder().id(1L).firstName("Old").lastName("Name").email("old@acme.com").build();
        UpdateCustomerInput input = mock(UpdateCustomerInput.class);
        when(input.getFirstName()).thenReturn("New");
        when(input.getLastName()).thenReturn(null);
        when(input.getEmail()).thenReturn("new@acme.com");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(existing)).thenReturn(existing);

        CustomerEntity result = customerService.updateCustomer(1L, input).block();

        assertEquals("New", result.getFirstName());
        assertEquals("Name", result.getLastName());
        assertEquals("new@acme.com", result.getEmail());
    }

    @Test
    void deleteCustomerDeletesWhenExists() {
        when(customerRepository.existsById(1L)).thenReturn(true);

        Boolean deleted = customerService.deleteCustomer(1L).block();

        assertTrue(deleted);
        verify(customerRepository).deleteById(1L);
    }

    @Test
    void deleteCustomerThrowsWhenMissing() {
        when(customerRepository.existsById(1L)).thenReturn(false);

        Mono<Boolean> result = customerService.deleteCustomer(1L);
        assertThrows(ResourceNotFoundException.class, result::block);
        verify(customerRepository, never()).deleteById(anyLong());
    }
}
