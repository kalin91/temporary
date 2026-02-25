package com.demo.portfolio.api.dto;

import org.springframework.data.domain.PageRequest;

import com.demo.portfolio.api.generated.types.OrderStatus;

import jakarta.annotation.Nullable;
import lombok.NonNull;

/**
 * Composite DataLoader key used by
 * {@link com.demo.portfolio.api.fetcher.OrdersByCustomerDataLoader} to batch-fetch
 * orders per customer while preserving per-field filter and pagination arguments.
 *
 * <p>GraphQL-java dispatches all pending {@code Customer.orders} loads in a single
 * batch call. By encoding the full query context — customer ID, optional status filter,
 * and pagination — into this record, the DataLoader can push filtering and pagination
 * down to the database rather than applying them in-memory after a generic fetch.</p>
 *
 * <p>Because this record is used as a {@link java.util.Map} key inside the DataLoader
 * registry, it relies on the record's auto-generated {@code equals} and {@code hashCode}
 * implementations, which perform a field-wise comparison of all three components.</p>
 *
 * @param customerId  the ID of the customer whose orders are being requested; never {@code null}
 * @param status      optional GraphQL {@link OrderStatus} to filter orders;
 *                    {@code null} means no status filter is applied
 * @param pageRequest Spring Data pagination descriptor carrying the page index and page
 *                    size; never {@code null}
 */
public record OrdersByCustomerKey(@NonNull Long customerId, @Nullable OrderStatus status, @NonNull PageRequest pageRequest) {
}
