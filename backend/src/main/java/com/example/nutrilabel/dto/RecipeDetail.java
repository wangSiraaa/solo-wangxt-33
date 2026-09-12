package com.example.nutrilabel.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecipeDetail(
        Long id,
        String name,
        String description,
        BigDecimal defaultServingSizeG,
        BigDecimal totalWeightG,
        List<IngredientLineDto> ingredients) {

    public record IngredientLineDto(
            String ingredientName,
            BigDecimal amountG,
            /** Stored nutrients with unknown values for this ingredient. */
            List<String> unknownNutrients) {
    }
}
