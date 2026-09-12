package com.example.nutrilabel.calc;

import java.math.BigDecimal;

/**
 * Static definition of a nutrient, independent of persistence.
 *
 * @param storageUnit unit that totals are accumulated in (and the unit a
 *                    derived value is expressed in)
 * @param derivation  non-null when this nutrient is computed from another one
 */
public record NutrientDef(
        String code,
        String name,
        String storageUnit,
        int displayOrder,
        Derivation derivation) {

    public boolean isDerived() {
        return derivation != null;
    }

    /**
     * Linear derivation applied to UNROUNDED totals, e.g. salt equivalent =
     * sodium(mg) x 2.5 / 1000.
     */
    public record Derivation(String fromNutrientCode, BigDecimal multiplier, BigDecimal divisor) {
    }
}
