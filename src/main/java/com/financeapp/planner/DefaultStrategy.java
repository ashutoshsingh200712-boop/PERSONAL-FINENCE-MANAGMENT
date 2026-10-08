package com.financeapp.planner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default allocation strategy using standard weights:
 * Food (40%), Savings (20%), Emergency (15%), Entertainment (10%), Other (15%).
 * Preserves insertion order via LinkedHashMap and ensures the last category absorbs
 * any fractional rounding so that sum of allocations strictly matches available funds.
 */
public class DefaultStrategy implements AllocationStrategy {

    private final Map<String, BigDecimal> categoryWeights;

    /**
     * Initializes strategy with default standard 40/20/15/10/15 weights.
     */
    public DefaultStrategy() {
        Map<String, BigDecimal> defaults = new LinkedHashMap<>();
        defaults.put("Food", new BigDecimal("0.40"));
        defaults.put("Savings", new BigDecimal("0.20"));
        defaults.put("Emergency", new BigDecimal("0.15"));
        defaults.put("Entertainment", new BigDecimal("0.10"));
        defaults.put("Other", new BigDecimal("0.15"));
        this.categoryWeights = Collections.unmodifiableMap(defaults);
    }

    /**
     * Initializes strategy with custom weights (e.g., loaded from system_settings).
     *
     * @param weights map of category name to decimal weight (e.g., 0.40 for 40%)
     */
    public DefaultStrategy(Map<String, BigDecimal> weights) {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("Category weights map cannot be null or empty");
        }
        this.categoryWeights = Collections.unmodifiableMap(new LinkedHashMap<>(weights));
    }

    @Override
    public Map<String, BigDecimal> allocate(BigDecimal available, BigDecimal savingsGoal) {
        if (available == null || available.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Available income cannot be null or negative");
        }

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal scaledAvailable = available.setScale(2, RoundingMode.HALF_UP);

        if (scaledAvailable.compareTo(BigDecimal.ZERO) == 0) {
            for (String category : categoryWeights.keySet()) {
                result.put(category, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }
            return result;
        }

        BigDecimal allocatedSoFar = BigDecimal.ZERO;
        int totalCategories = categoryWeights.size();
        int index = 0;

        Iterator<Map.Entry<String, BigDecimal>> it = categoryWeights.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, BigDecimal> entry = it.next();
            index++;
            String category = entry.getKey();
            BigDecimal weight = entry.getValue();

            if (index == totalCategories) {
                // Last category absorbs any rounding difference to ensure exact match
                BigDecimal remainder = scaledAvailable.subtract(allocatedSoFar);
                result.put(category, remainder.setScale(2, RoundingMode.HALF_UP));
            } else {
                BigDecimal categoryAmount = scaledAvailable.multiply(weight).setScale(2, RoundingMode.HALF_UP);
                result.put(category, categoryAmount);
                allocatedSoFar = allocatedSoFar.add(categoryAmount);
            }
        }

        return result;
    }

    public Map<String, BigDecimal> getCategoryWeights() {
        return categoryWeights;
    }
}
