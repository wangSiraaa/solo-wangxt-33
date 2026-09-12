package com.example.nutrilabel.calc;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Exact BigDecimal conversions between the units used by this application.
 * Factors are exact decimal constants (no binary floating point anywhere).
 */
public final class UnitConverter {

    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal KCAL_TO_KJ = new BigDecimal("4.184");

    /** key: from + "->" + to, value: multiplication factor */
    private static final Map<String, BigDecimal> FACTORS = Map.of(
            "g->mg", THOUSAND,
            "mg->g", new BigDecimal("0.001"),
            "kcal->kJ", KCAL_TO_KJ,
            "kJ->kcal", BigDecimal.ONE.divide(KCAL_TO_KJ, 34, java.math.RoundingMode.HALF_UP));

    private UnitConverter() {
    }

    public static BigDecimal convert(BigDecimal value, String fromUnit, String toUnit) {
        if (fromUnit.equals(toUnit)) {
            return value;
        }
        BigDecimal factor = FACTORS.get(fromUnit + "->" + toUnit);
        if (factor == null) {
            throw new IllegalArgumentException(
                    "No conversion defined from " + fromUnit + " to " + toUnit);
        }
        return value.multiply(factor);
    }
}
