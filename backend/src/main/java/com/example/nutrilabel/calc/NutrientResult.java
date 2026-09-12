package com.example.nutrilabel.calc;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full result for one nutrient: unrounded totals plus both display columns.
 *
 * @param totalUnrounded    unrounded total for the whole recipe, in the storage
 *                          unit (partial when dataComplete is false)
 * @param missingIngredients ingredients with unknown values for this nutrient
 * @param contributions     per-ingredient audit trail
 */
public record NutrientResult(
        String code,
        String name,
        String displayUnit,
        boolean dataComplete,
        BigDecimal totalUnrounded,
        ColumnValue per100g,
        ColumnValue perServing,
        List<String> missingIngredients,
        List<Contribution> contributions) {
}
