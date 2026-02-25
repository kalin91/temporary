package com.demo.portfolio.api.fetcher;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.domain.OrderEntity;
import com.demo.portfolio.api.dto.OrdersByCustomerKey;
import com.demo.portfolio.api.generated.types.Order;
import com.demo.portfolio.api.generated.types.OrderStatus;
import com.demo.portfolio.api.mapper.OrderMapper;
import com.demo.portfolio.api.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdersByCustomerDataLoaderTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrdersByCustomerDataLoader loader;

    @Test
    void loadReturnsGroupedOrdersAndEmptyForMissing() throws Exception {
        PageRequest pageRequest = PageRequest.of(0, 5);
        OrdersByCustomerKey key1 = new OrdersByCustomerKey(1L, OrderStatus.PENDING, pageRequest);
        OrdersByCustomerKey key2 = new OrdersByCustomerKey(2L, OrderStatus.PENDING, pageRequest);

        OrderEntity entity = OrderEntity.builder().id(10L).customer(CustomerEntity.builder().id(1L).build()).build();
        Order dto = mock(Order.class);
        com.demo.portfolio.api.generated.types.Customer customerDto = mock(com.demo.portfolio.api.generated.types.Customer.class);
        when(customerDto.getId()).thenReturn("1");
        when(dto.getCustomer()).thenReturn(customerDto);

        when(orderMapper.toEntity(OrderStatus.PENDING)).thenReturn(com.demo.portfolio.api.domain.OrderStatus.PENDING);
        when(orderRepository.findByCustomerIdInAndOptionalStatus(Set.of(1L, 2L), com.demo.portfolio.api.domain.OrderStatus.PENDING, pageRequest))
                .thenReturn(List.of(entity));
        when(orderMapper.toDto(entity)).thenReturn(dto);

        Map<OrdersByCustomerKey, List<Order>> result = loader.load(Set.of(key1, key2)).toCompletableFuture().get();

        assertEquals(2, result.size());
        assertEquals(1, result.get(key1).size());
        assertTrue(result.get(key2).isEmpty());
    }
}
