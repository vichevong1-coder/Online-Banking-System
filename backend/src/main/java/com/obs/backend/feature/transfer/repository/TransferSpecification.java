package com.obs.backend.feature.transfer.repository;

import com.obs.backend.feature.account.entity.Currency;
import com.obs.backend.feature.transfer.entity.Transfer;
import com.obs.backend.feature.transfer.entity.TransferStatus;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class TransferSpecification {

    private TransferSpecification() {}

    public static Specification<Transfer> filterBy(
            Instant fromDate,
            Instant toDate,
            boolean toDateInclusive,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            TransferStatus status,
            Currency currency,
            UUID accountId,
            Collection<UUID> matchingAccountIds,
            String externalRefPattern) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                if (toDateInclusive) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
                } else {
                    predicates.add(cb.lessThan(root.get("createdAt"), toDate));
                }
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (currency != null) {
                predicates.add(cb.equal(root.get("currency"), currency));
            }

            if (accountId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("fromAccountId"), accountId),
                        cb.equal(root.get("toAccountId"), accountId)
                ));
            }

            if (matchingAccountIds != null && !matchingAccountIds.isEmpty()) {
                Predicate accountInFrom = root.get("fromAccountId").in(matchingAccountIds);
                Predicate accountInTo = root.get("toAccountId").in(matchingAccountIds);
                if (externalRefPattern != null && !externalRefPattern.isBlank()) {
                    Predicate extMatch = cb.like(cb.lower(root.get("externalRef")), "%" + externalRefPattern.toLowerCase() + "%");
                    predicates.add(cb.or(accountInFrom, accountInTo, extMatch));
                } else {
                    predicates.add(cb.or(accountInFrom, accountInTo));
                }
            } else if (externalRefPattern != null && !externalRefPattern.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("externalRef")), "%" + externalRefPattern.toLowerCase() + "%"));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
