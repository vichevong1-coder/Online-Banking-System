package com.obs.backend.feature.transfer.config;

import com.obs.backend.feature.account.entity.Currency;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * US-027 transfer caps, keyed by currency.
 *
 * <p>Caps are per-currency rather than a single pair of numbers because a
 * USD-calibrated limit is nonsense against KHR: a flat cap of 5,000 would reject
 * a KHR transfer worth about a dollar. Cross-currency transfers are rejected
 * (see {@code sprint4-todolist.md}), so every transfer has exactly one currency
 * and exactly one pair of caps applies to it.
 */
@Component
@ConfigurationProperties(prefix = "transfer.limits")
public class TransferLimitProperties {

    private Map<Currency, CurrencyLimits> perCurrency = new EnumMap<>(Currency.class);

    public Map<Currency, CurrencyLimits> getPerCurrency() {
        return perCurrency;
    }

    public void setPerCurrency(Map<Currency, CurrencyLimits> perCurrency) {
        this.perCurrency = perCurrency;
    }

    /**
     * Fails loudly rather than returning null: a missing entry would otherwise
     * surface as an NPE in the middle of the transfer transaction.
     */
    public CurrencyLimits forCurrency(Currency currency) {
        CurrencyLimits limits = perCurrency.get(currency);
        if (limits == null || limits.getPerTransfer() == null || limits.getDaily() == null) {
            throw new IllegalStateException(
                    "No transfer limits configured for currency " + currency
                            + " — set transfer.limits.per-currency." + currency.name().toLowerCase()
                            + ".per-transfer and .daily");
        }
        return limits;
    }

    public static class CurrencyLimits {

        /** Rejects a single transfer larger than this. */
        private BigDecimal perTransfer;

        /** Rejects a transfer that would push the day's COMPLETED total past this. */
        private BigDecimal daily;

        public BigDecimal getPerTransfer() {
            return perTransfer;
        }

        public void setPerTransfer(BigDecimal perTransfer) {
            this.perTransfer = perTransfer;
        }

        public BigDecimal getDaily() {
            return daily;
        }

        public void setDaily(BigDecimal daily) {
            this.daily = daily;
        }
    }
}
