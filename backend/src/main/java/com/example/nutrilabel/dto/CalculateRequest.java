package com.example.nutrilabel.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record CalculateRequest(
        @NotNull Long recipeId,
        /** @Valid cascades bean validation into each scenario element. */
        @NotEmpty @Valid List<Scenario> scenarios) {

    public record Scenario(
            @NotNull Long ruleVersionId,
            @Positive BigDecimal servingSizeG) {
    }
}
