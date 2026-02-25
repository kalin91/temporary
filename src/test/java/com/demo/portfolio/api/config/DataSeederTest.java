package com.demo.portfolio.api.config;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.domain.OrderEntity;
import com.demo.portfolio.api.repository.CustomerRepository;
import com.demo.portfolio.api.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

        dataSeeder.run();

        verify(customerRepository, never()).saveAll(any());
        verify(orderRepository, never()).saveAll(any());
    }

    @Test
    void runSeedsDataWhenEmpty() {
        DataSeeder seeder = new DataSeeder(customerRepository, orderRepository, new ObjectMapper());
        when(customerRepository.count()).thenReturn(0L);
        when(customerRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        seeder.run();

        ArgumentCaptor<List<CustomerEntity>> customerCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<OrderEntity>> orderCaptor = ArgumentCaptor.forClass(List.class);
        verify(customerRepository).saveAll(customerCaptor.capture());
        verify(orderRepository).saveAll(orderCaptor.capture());
        assertFalse(customerCaptor.getValue().isEmpty());
        assertFalse(orderCaptor.getValue().isEmpty());
    }
}
