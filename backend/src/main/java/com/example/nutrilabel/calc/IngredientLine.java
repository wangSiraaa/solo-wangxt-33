package com.example.nutrilabel.calc;

import java.math.BigDecimal;
import java.util.Map;

/**
 * One ingredient line of a recipe.
 *
 * @param knownValuesPer100g nutrient code -> value per 100 g (storage unit).
 *                           Only KNOWN values are present; a missing key is a
 *                           data gap and must never be read as zero.
 */
public record IngredientLine(
        String ingredientName,
        BigDecimal amountG,
        Map<String, BigDecimal> knownValuesPer100g) {
}
