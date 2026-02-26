package com.demo.portfolio.api.config;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.domain.OrderEntity;
import com.demo.portfolio.api.repository.CustomerRepository;
import com.demo.portfolio.api.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class DataSeederTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void runSkipsWhenCustomersExist() {
        when(customerRepository.count()).thenReturn(1L);

        dataSeeder.afterSingletonsInstantiated();

        verify(customerRepository, never()).saveAll(any());
        verify(orderRepository, never()).saveAll(any());
    }

    @Test
    void runSeedsDataWhenEmpty() {
        DataSeeder seeder = new DataSeeder(customerRepository, orderRepository, new ObjectMapper());
        when(customerRepository.count()).thenReturn(0L);
        when(customerRepository.saveAll(any())).thenAnswer(i -> i.<Iterable<CustomerEntity>>getArgument(0));
        when(orderRepository.saveAll(any())).thenAnswer(i -> i.<Iterable<OrderEntity>>getArgument(0));

        seeder.afterSingletonsInstantiated();

        verify(customerRepository).saveAll(argThat(customers -> {
            assertTrue(customers.iterator().hasNext());
            return true;
        }));
        verify(orderRepository).saveAll(argThat(orders -> {
            assertTrue(orders.iterator().hasNext());
            return true;
        }));
    }
}
