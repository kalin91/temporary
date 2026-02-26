package com.demo.portfolio.api.fetcher;

import com.demo.portfolio.api.domain.OrderEntity;
import com.demo.portfolio.api.mapper.OrderMapper;
import com.demo.portfolio.api.domain.OrderStatus;
import com.demo.portfolio.api.dto.OrdersByCustomerKey;
import com.demo.portfolio.api.generated.types.Order;
import com.demo.portfolio.api.repository.OrderRepository;
import com.netflix.graphql.dgs.DgsDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.MappedBatchLoader;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * DGS DataLoader that batch-fetches {@link Order} DTOs grouped by a composite
 * {@link OrdersByCustomerKey} (customer ID + optional status + pagination).
 *
 * <p>
 * Resolves the {@code Customer.orders} field by collecting all
 * {@link OrdersByCustomerKey} instances accumulated within a single GraphQL execution
 * tick, extracting the unique customer IDs, and issuing a single
 * {@code SELECT … WHERE customer_id IN (…) AND (:status IS NULL OR status = :status)}
 * query via {@link com.demo.portfolio.api.repository.OrderRepository#findByCustomerIdInAndOptionalStatus}.
 * This eliminates the N+1 problem that would otherwise occur when a list of customers
 * is returned alongside their orders.
 * </p>
 *
 * <h2>Batching behaviour</h2>
 * <p>
 * Filtering by {@code status} and offset-based pagination ({@code page}/{@code size})
 * are pushed to the database query rather than applied in-memory. Because GraphQL-java
 * dispatches all pending loads in one call, the {@code status} and {@code pageRequest}
 * values are taken from an arbitrary key in the batch via {@link java.util.Set#stream()
 * stream().findAny()}. This is appropriate when all customers in a single query share
 * the same filter/pagination arguments, which is the standard use-case for this field.
 * </p>
 * <p>
 * Each requested {@link OrdersByCustomerKey} is guaranteed an entry in the result map;
 * keys without matching orders receive an empty list so the DataLoader never resolves
 * to {@code null} for a valid customer.
 * </p>
 *
 * @see CustomerByIdDataLoader
 * @see OrdersByCustomerKey
 */
@Slf4j
@Component
@DgsDataLoader(name = "ordersByCustomer")
@RequiredArgsConstructor
public class OrdersByCustomerDataLoader implements MappedBatchLoader<OrdersByCustomerKey, List<Order>> {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    /**
     * Batch-loads {@link Order} DTOs for all {@link OrdersByCustomerKey} instances
     * accumulated during the current GraphQL execution tick.
     *
     * <p>The unique customer IDs are extracted from {@code keys} and passed together to
     * {@link com.demo.portfolio.api.repository.OrderRepository#findByCustomerIdInAndOptionalStatus},
     * which issues a single SQL query with an optional {@code status} predicate and
     * offset-based pagination. The resulting {@link OrderEntity} list is mapped to DTOs
     * via {@link OrderMapper#toDto} and then grouped by customer ID.  Finally, every
     * input key is guaranteed an entry in the returned map (empty list when no orders
     * were found) so the DataLoader never resolves to {@code null}.</p>
     *
     * <p><strong>Note:</strong> {@code status} and {@code pageRequest} are shared across
     * the entire batch (taken from an arbitrary key). This matches the expected usage
     * where all customers in one query share the same filter/pagination arguments.</p>
     *
     * @param keys the set of composite keys accumulated in the current GraphQL tick;
     *             each key carries a {@code customerId}, an optional {@code status}, and
     *             a {@code pageRequest}
     * @return a {@link CompletionStage} resolving to a map of
     *         {@code OrdersByCustomerKey → List<Order>}; every input key has an entry
     */
    @Override
    public CompletionStage<Map<OrdersByCustomerKey, List<Order>>> load(Set<OrdersByCustomerKey> keys) {
        Set<Long> customerIds = keys.stream()
            .map(OrdersByCustomerKey::customerId)
            .collect(Collectors.toSet());
        PageRequest pageRequest = keys.stream().findAny().map(OrdersByCustomerKey::pageRequest).orElseThrow();
        OrderStatus status = orderMapper.toEntity(keys.stream().findAny().map(OrdersByCustomerKey::status).orElse(null));
        log.debug("Batch-loading orders for {} customer IDs: {}", customerIds.size(), customerIds);
        return executeBlocking(() -> {
            List<OrderEntity> all = orderRepository.findByCustomerIdInAndOptionalStatus(customerIds, status, pageRequest);

            // Group results by customer ID. The proxy getId() call does NOT trigger
            // a lazy-load: Hibernate embeds the FK value in the proxy itself.
            Map<Long, List<Order>> grouped = all.stream()
                .map(orderMapper::toDto)
                .collect(Collectors.groupingBy(order -> Long.parseLong(order.getCustomer().getId())));

            // Ensure every requested customer ID has an entry (even if empty),
            // so the DataLoader never resolves to null for a valid customer.
            Map<OrdersByCustomerKey, List<Order>> result = keys.stream()
                .collect(Collectors.toMap(
                    key -> key,
                    key -> grouped.getOrDefault(key.customerId(), List.of())
                ));

            log.debug("Batch result: {} orders across {} customers", all.size(), grouped.size());
            return result;
        }).doOnError(ex -> log.error(
            "OrdersByCustomerDataLoader.load failed for {} keys ({} customer ids)",
            keys.size(),
            customerIds.size(),
            ex))
          .toFuture();
    }

    /**
     * Executes blocking loader work on {@code boundedElastic}.
     *
     * @param action blocking action
     * @param <T> result type
     * @return a {@link Mono} scheduled on {@code boundedElastic}
     */
    private <T> Mono<T> executeBlocking(Callable<T> action) {
        return Mono.fromCallable(action)
            .subscribeOn(Schedulers.boundedElastic());
    }
}
