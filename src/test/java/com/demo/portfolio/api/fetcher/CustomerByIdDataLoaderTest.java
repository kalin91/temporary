package com.demo.portfolio.api.fetcher;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerByIdDataLoaderTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerByIdDataLoader loader;

    @Test
    void loadReturnsEntitiesMappedById() throws Exception {
        CustomerEntity customer = CustomerEntity.builder().id(1L).build();
        when(customerRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(customer));

        Map<Long, CustomerEntity> result = loader.load(Set.of(1L, 2L)).toCompletableFuture().get();

        assertEquals(1, result.size());
        assertEquals(customer, result.get(1L));
    }
}
