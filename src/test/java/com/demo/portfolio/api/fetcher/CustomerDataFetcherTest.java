package com.demo.portfolio.api.fetcher;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.generated.types.Customer;
import com.demo.portfolio.api.mapper.CustomerMapper;
import com.demo.portfolio.api.service.CustomerService;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {DgsAutoConfiguration.class, CustomerDataFetcher.class})
class CustomerDataFetcherTest {

    @Autowired
    private DgsQueryExecutor dgsQueryExecutor;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private CustomerMapper customerMapper;

    @Test
    void customer_ShouldReturnCustomer() {
        CustomerEntity entity = new CustomerEntity(1L, "John", "Doe", "john@example.com", null);
        Customer dto = new Customer("1", "John", "Doe", "john@example.com", null);

        when(customerService.getCustomer(anyLong())).thenReturn(Mono.just(entity));
        when(customerMapper.toDto(entity)).thenReturn(dto);

        String query = """
                query {
                    customer(id: "1") {
                        id
                        firstName
                        lastName
                        email
                    }
                }
                """;

        String firstName = dgsQueryExecutor.executeAndExtractJsonPath(query, "data.customer.firstName");
        assertEquals("John", firstName);
    }
}
