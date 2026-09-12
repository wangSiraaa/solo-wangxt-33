package com.example.nutrilabel.calc;

import java.math.BigDecimal;
import java.util.List;

/**
 * A complete label for one scenario (recipe x rule version x serving size).
 */
public record LabelResult(
        String recipeName,
        BigDecimal totalWeightG,
        BigDecimal servingSizeG,
        String ruleSetCode,
        String ruleVersion,
        String disclaimer,
        List<NutrientResult> nutrients) {
}
