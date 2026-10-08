package com.financeapp.planner;

import java.math.BigDecimal;

/**
 * Status evaluating the trajectory / pace of user category expenditure relative to expected budget pace.
 * - UNDER: spending ratio &lt; 80% (surplus available)
 * - ON_TRACK: spending ratio between 80% and 110% (sustainable pace)
 * - OVER: spending ratio &gt; 110% (overspending risk)
 */
public enum PlanStatus {
    UNDER,
    ON_TRACK,
    OVER;

    private static final BigDecimal UNDER_THRESHOLD = new BigDecimal("0.80");
    private static final BigDecimal OVER_THRESHOLD = new BigDecimal("1.10");

    /**
     * Determines status from spending pace ratio (spent / expected_by_date).
     *
     * @param ratio spending pace ratio
     * @return appropriate PlanStatus
     */
    public static PlanStatus fromRatio(BigDecimal ratio) {
        if (ratio == null) {
            return ON_TRACK;
        }
        if (ratio.compareTo(UNDER_THRESHOLD) < 0) {
            return UNDER;
        } else if (ratio.compareTo(OVER_THRESHOLD) > 0) {
            return OVER;
        } else {
            return ON_TRACK;
        }
    }
}
