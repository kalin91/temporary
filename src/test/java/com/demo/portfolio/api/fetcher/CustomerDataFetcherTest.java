package com.demo.portfolio.api.fetcher;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.dto.OrdersByCustomerKey;
import com.demo.portfolio.api.generated.types.*;
import com.demo.portfolio.api.mapper.CustomerMapper;
import com.demo.portfolio.api.service.CustomerService;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import org.dataloader.DataLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CustomerDataFetcherTest {

    @Mock
    private CustomerService customerService;
    @Mock
    private CustomerMapper customerMapper;

    @Test
    void customersMapsPage() {
        CustomerDataFetcher fetcher = new CustomerDataFetcher(customerService, customerMapper);
        CustomerEntity entity = CustomerEntity.builder().id(1L).build();
        Customer dto = mock(Customer.class);
        when(customerService.getCustomers(0, 10)).thenReturn(Mono.just(new PageImpl<>(List.of(entity))));
        when(customerMapper.toDto(entity)).thenReturn(dto);

        CustomerPage page = fetcher.customers(null, null).block();

        assertNotNull(page);
        assertEquals(1, page.getContent().size());
    }

    @Test
    void customerDelegatesToServiceAndMapper() {
        CustomerDataFetcher fetcher = new CustomerDataFetcher(customerService, customerMapper);
        CustomerEntity entity = CustomerEntity.builder().id(1L).build();
        Customer dto = mock(Customer.class);
        when(customerService.getCustomer(1L)).thenReturn(Mono.just(entity));
        when(customerMapper.toDto(entity)).thenReturn(dto);

        assertSame(dto, fetcher.customer("1").block());
    }

    @Test
    void createCustomerDelegatesToServiceAndMapper() {
        CustomerDataFetcher fetcher = new CustomerDataFetcher(customerService, customerMapper);
        CreateCustomerInput input = mock(CreateCustomerInput.class);
        CustomerEntity entity = CustomerEntity.builder().id(1L).build();
        Customer dto = mock(Customer.class);
        when(customerService.createCustomer(input)).thenReturn(Mono.just(entity));
        when(customerMapper.toDto(entity)).thenReturn(dto);

        assertSame(dto, fetcher.createCustomer(input).block());
    }

    @Test
    void updateCustomerDelegatesToServiceAndMapper() {
        CustomerDataFetcher fetcher = new CustomerDataFetcher(customerService, customerMapper);
        UpdateCustomerInput input = mock(UpdateCustomerInput.class);
        CustomerEntity entity = CustomerEntity.builder().id(1L).build();
        Customer dto = mock(Customer.class);
        when(customerService.updateCustomer(1L, input)).thenReturn(Mono.just(entity));
        when(customerMapper.toDto(entity)).thenReturn(dto);

        assertSame(dto, fetcher.updateCustomer("1", input).block());
    }

    @Test
    void deleteCustomerDelegatesToService() {
        CustomerDataFetcher fetcher = new CustomerDataFetcher(customerService, customerMapper);
        when(customerService.deleteCustomer(1L)).thenReturn(Mono.just(true));

        assertTrue(fetcher.deleteCustomer("1").block());
    }

    @Test
    void ordersForCustomerUsesDataLoader() {
        CustomerDataFetcher fetcher = new CustomerDataFetcher(customerService, customerMapper);
        DgsDataFetchingEnvironment dfe = mock(DgsDataFetchingEnvironment.class);
        DataLoader<OrdersByCustomerKey, List<Order>> loader = mock(DataLoader.class);
        Customer source = mock(Customer.class);
        Order order = mock(Order.class);

        when(source.getId()).thenReturn("1");
        when(dfe.getSource()).thenReturn(source);
        when(dfe.getDataLoader(OrdersByCustomerDataLoader.class)).thenReturn(loader);
        when(loader.load(any())).thenReturn(CompletableFuture.completedFuture(List.of(order)));

        List<Order> orders = fetcher.ordersForCustomer(dfe, 0, 10, OrderStatus.PENDING).block();

        assertEquals(1, orders.size());
    }
}
