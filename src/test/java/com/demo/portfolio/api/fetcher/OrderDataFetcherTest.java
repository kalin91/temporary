package com.demo.portfolio.api.fetcher;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.domain.OrderEntity;
import com.demo.portfolio.api.generated.types.*;
import com.demo.portfolio.api.mapper.CustomerMapper;
import com.demo.portfolio.api.mapper.OrderMapper;
import com.demo.portfolio.api.service.OrderService;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDataFetcherTest {

    @Mock
    private OrderService orderService;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private CustomerMapper customerMapper;

    @Test
    void ordersMapsPage() {
        OrderDataFetcher fetcher = new OrderDataFetcher(orderService, orderMapper, customerMapper);
        OrderEntity entity = OrderEntity.builder().id(1L).build();
        Order dto = mock(Order.class);

        when(orderMapper.toEntity(OrderStatus.PENDING)).thenReturn(com.demo.portfolio.api.domain.OrderStatus.PENDING);
        when(orderService.getOrders(1L, com.demo.portfolio.api.domain.OrderStatus.PENDING, 0, 10))
                .thenReturn(Mono.just(new PageImpl<>(List.of(entity))));
        when(orderMapper.toDto(entity)).thenReturn(dto);

        OrderPage result = fetcher.orders("1", OrderStatus.PENDING, null, null).block();

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void orderDelegatesToServiceAndMapper() {
        OrderDataFetcher fetcher = new OrderDataFetcher(orderService, orderMapper, customerMapper);
        OrderEntity entity = OrderEntity.builder().id(1L).build();
        Order dto = mock(Order.class);
        when(orderService.getOrder(1L)).thenReturn(Mono.just(entity));
        when(orderMapper.toDto(entity)).thenReturn(dto);

        assertSame(dto, fetcher.order("1").block());
    }

    @Test
    void createOrderDelegatesToServiceAndMapper() {
        OrderDataFetcher fetcher = new OrderDataFetcher(orderService, orderMapper, customerMapper);
        CreateOrderInput input = mock(CreateOrderInput.class);
        OrderEntity entity = OrderEntity.builder().id(1L).build();
        Order dto = mock(Order.class);
        when(orderService.createOrder(input)).thenReturn(Mono.just(entity));
        when(orderMapper.toDto(entity)).thenReturn(dto);

        assertSame(dto, fetcher.createOrder(input).block());
    }

    @Test
    void updateOrderDelegatesToServiceAndMapper() {
        OrderDataFetcher fetcher = new OrderDataFetcher(orderService, orderMapper, customerMapper);
        UpdateOrderInput input = mock(UpdateOrderInput.class);
        OrderEntity entity = OrderEntity.builder().id(1L).build();
        Order dto = mock(Order.class);
        when(orderService.updateOrder(1L, input)).thenReturn(Mono.just(entity));
        when(orderMapper.toDto(entity)).thenReturn(dto);

        assertSame(dto, fetcher.updateOrder("1", input).block());
    }

    @Test
    void deleteOrderDelegatesToService() {
        OrderDataFetcher fetcher = new OrderDataFetcher(orderService, orderMapper, customerMapper);
        when(orderService.deleteOrder(1L)).thenReturn(Mono.just(true));

        assertTrue(fetcher.deleteOrder("1").block());
    }

    @Test
    void customerForOrderUsesDataLoader() {
        OrderDataFetcher fetcher = new OrderDataFetcher(orderService, orderMapper, customerMapper);
        DgsDataFetchingEnvironment dfe = mock(DgsDataFetchingEnvironment.class);
        DataLoader<Long, CustomerEntity> loader = mock(DataLoader.class);
        Order sourceOrder = mock(Order.class);
        Customer partialCustomer = mock(Customer.class);
        Customer mapped = mock(Customer.class);
        CustomerEntity customerEntity = CustomerEntity.builder().id(1L).build();

        when(partialCustomer.getId()).thenReturn("1");
        when(sourceOrder.getCustomer()).thenReturn(partialCustomer);
        when(dfe.getSource()).thenReturn(sourceOrder);
        when(dfe.getDataLoader(CustomerByIdDataLoader.class)).thenReturn(loader);
        when(loader.load(1L)).thenReturn(CompletableFuture.completedFuture(customerEntity));
        when(customerMapper.toDto(customerEntity)).thenReturn(mapped);

        assertSame(mapped, fetcher.customerForOrder(dfe).block());
    }
}
