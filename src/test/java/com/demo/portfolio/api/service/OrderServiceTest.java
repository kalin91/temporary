package com.demo.portfolio.api.service;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.domain.OrderEntity;
import com.demo.portfolio.api.domain.OrderStatus;
import com.demo.portfolio.api.exception.ResourceNotFoundException;
import com.demo.portfolio.api.generated.types.CreateOrderInput;
import com.demo.portfolio.api.generated.types.UpdateOrderInput;
import com.demo.portfolio.api.repository.CustomerRepository;
import com.demo.portfolio.api.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void getOrdersUsesCombinedFilter() {
        Page<OrderEntity> page = new PageImpl<>(List.of());
        when(orderRepository.findByCustomerIdAndStatus(eq(1L), eq(OrderStatus.PENDING), any(PageRequest.class))).thenReturn(page);

        Page<OrderEntity> result = orderService.getOrders(1L, OrderStatus.PENDING, 0, 5).block();

        assertSame(page, result);
    }

    @Test
    void getOrdersUsesStatusFilterOnly() {
        Page<OrderEntity> page = new PageImpl<>(List.of());
        when(orderRepository.findByStatus(OrderStatus.PENDING, PageRequest.of(0, 5))).thenReturn(page);

        Page<OrderEntity> result = orderService.getOrders(null, OrderStatus.PENDING, 0, 5).block();

        assertSame(page, result);
    }

    @Test
    void getOrdersUsesCustomerFilterOnly() {
        Page<OrderEntity> page = new PageImpl<>(List.of());
        when(orderRepository.findByCustomerId(1L, PageRequest.of(0, 5))).thenReturn(page);

        Page<OrderEntity> result = orderService.getOrders(1L, null, 0, 5).block();

        assertSame(page, result);
    }

    @Test
    void getOrdersUsesFindAllWithoutFilters() {
        Page<OrderEntity> page = new PageImpl<>(List.of());
        when(orderRepository.findAll(PageRequest.of(0, 5))).thenReturn(page);

        Page<OrderEntity> result = orderService.getOrders(null, null, 0, 5).block();

        assertSame(page, result);
    }

    @Test
    void getOrderReturnsEntityWhenFound() {
        OrderEntity entity = OrderEntity.builder().id(1L).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertSame(entity, orderService.getOrder(1L).block());
    }

    @Test
    void getOrderThrowsWhenMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        Mono<OrderEntity> result = orderService.getOrder(99L);
        assertThrows(ResourceNotFoundException.class, result::block);
    }

    @Test
    void createOrderSavesPendingOrder() {
        CreateOrderInput input = mock(CreateOrderInput.class);
        when(input.getCustomerId()).thenReturn("2");
        when(input.getTotalAmount()).thenReturn(50.0);
        CustomerEntity customer = CustomerEntity.builder().id(2L).build();
        when(customerRepository.findById(2L)).thenReturn(Optional.of(customer));

        OrderEntity saved = OrderEntity.builder().id(1L).status(OrderStatus.PENDING).customer(customer).build();
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(saved);

        OrderEntity result = orderService.createOrder(input).block();

        assertSame(saved, result);
        verify(orderRepository).save(argThat(e -> e.getStatus() == OrderStatus.PENDING && e.getCustomer() == customer));
    }

    @Test
    void updateOrderUpdatesFields() {
        OrderEntity existing = OrderEntity.builder().id(1L).status(OrderStatus.PENDING).totalAmount(10.0).build();
        UpdateOrderInput input = mock(UpdateOrderInput.class);
        com.demo.portfolio.api.generated.types.OrderStatus gqlStatus = mock(com.demo.portfolio.api.generated.types.OrderStatus.class);
        when(gqlStatus.name()).thenReturn("SHIPPED");
        when(input.getStatus()).thenReturn(gqlStatus);
        when(input.getTotalAmount()).thenReturn(20.0);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(existing)).thenReturn(existing);

        OrderEntity result = orderService.updateOrder(1L, input).block();

        assertEquals(OrderStatus.SHIPPED, result.getStatus());
        assertEquals(20.0, result.getTotalAmount());
    }

    @Test
    void deleteOrderDeletesWhenExists() {
        when(orderRepository.existsById(1L)).thenReturn(true);

        assertTrue(orderService.deleteOrder(1L).block());
        verify(orderRepository).deleteById(1L);
    }

    @Test
    void deleteOrderThrowsWhenMissing() {
        when(orderRepository.existsById(1L)).thenReturn(false);

        Mono<Boolean> result = orderService.deleteOrder(1L);
        assertThrows(ResourceNotFoundException.class, result::block);
    }
}
