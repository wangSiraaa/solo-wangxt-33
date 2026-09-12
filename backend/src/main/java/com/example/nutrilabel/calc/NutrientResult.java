package com.example.nutrilabel.calc;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full result for one nutrient: unrounded totals plus both display columns.
 *
 * @param rule              the rule parameters actually applied to this nutrient
 *                          (kept on the result so exports can state the exact basis)
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
        RuleDef rule,
        BigDecimal totalUnrounded,
        ColumnValue per100g,
        ColumnValue perServing,
        List<String> missingIngredients,
        List<Contribution> contributions) {
}
