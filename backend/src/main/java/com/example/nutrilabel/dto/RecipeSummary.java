package com.example.nutrilabel.dto;

import java.math.BigDecimal;

public record RecipeSummary(
        Long id,
        String name,
        String description,
        BigDecimal defaultServingSizeG,
        BigDecimal totalWeightG) {
}
