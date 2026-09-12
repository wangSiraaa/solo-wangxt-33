package com.example.nutrilabel.calc;

import java.math.BigDecimal;

/**
 * Audit trail entry: how much one ingredient contributed to one nutrient total.
 *
 * @param valuePer100g        known value per 100 g; null when unknown (gap)
 * @param contributionToTotal unrounded contribution to the recipe total, in the
 *                            nutrient's storage unit; null when unknown
 */
public record Contribution(
        String ingredientName,
        BigDecimal amountG,
        BigDecimal valuePer100g,
        BigDecimal contributionToTotal) {
}
