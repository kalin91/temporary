package com.demo.portfolio.api.fetcher;

import com.demo.portfolio.api.domain.CustomerEntity;
import com.demo.portfolio.api.repository.CustomerRepository;
import com.netflix.graphql.dgs.DgsDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.MappedBatchLoader;
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
 * DGS DataLoader that batch-fetches {@link CustomerEntity} records by their primary key.
 *
 * <p>Resolves the {@code Order.customer} field by accumulating all customer IDs requested
 * within a single GraphQL execution tick and issuing one
 * {@code SELECT … WHERE id IN (…)} query via {@link CustomerRepository#findAllById},
 * eliminating the N+1 problem that would otherwise arise when resolving the customer
 * for each order in a list.</p>
 *
 * <h2>Batching behaviour</h2>
 * <p>The loader never returns {@code null} for a requested ID. If a customer ID is not
 * found in the database the entry is simply absent from the result map, which causes the
 * DataLoader to resolve that key to {@code null} and lets the GraphQL error-handling
 * layer surface the problem cleanly.</p>
 *
 * @see OrdersByCustomerDataLoader
 */
@Slf4j
@Component
@DgsDataLoader(name = "customerById")
@RequiredArgsConstructor
public class CustomerByIdDataLoader implements MappedBatchLoader<Long, CustomerEntity> {

    private final CustomerRepository customerRepository;

    /**
     * Batch-loads {@link CustomerEntity} records for all supplied {@code customerIds}
     * in a single {@code findAllById} query and returns them indexed by their ID.
     *
     * @param customerIds the set of customer IDs accumulated during the current GraphQL tick
     * @return a {@link CompletionStage} resolving to a {@code Map<Long, CustomerEntity>}
     *         containing only the IDs that were found in the database
     */
    @Override
    public CompletionStage<Map<Long, CustomerEntity>> load(Set<Long> customerIds) {
        log.debug("Batch-loading {} customers by ID: {}", customerIds.size(), customerIds);

        return executeBlocking(() -> {
            List<CustomerEntity> customers = customerRepository.findAllById(customerIds);

            Map<Long, CustomerEntity> result = customers.stream()
                    .collect(Collectors.toMap(CustomerEntity::getId, c -> c));

            log.debug("Batch result: {} customers found", result.size());
            return result;
        }).doOnError(ex -> log.error("CustomerByIdDataLoader.load failed for {} ids", customerIds.size(), ex))
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
