package com.example.nutrilabel.calc;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param totalWeightG weight of the whole recipe batch (finished yield)
 * @param servingSizeG serving size used for the per-serving column
 */
public record RecipeInput(
        String recipeName,
        BigDecimal totalWeightG,
        BigDecimal servingSizeG,
        List<IngredientLine> lines) {
}
