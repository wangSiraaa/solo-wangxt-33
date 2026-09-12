package com.example.nutrilabel.dto;

import com.example.nutrilabel.calc.LabelResult;
import com.example.nutrilabel.calc.NutrientResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Export payload: the label plus the full calculation basis, so that every
 * displayed number can be traced back to ingredient values and rule
 * parameters.
 */
public record LabelExport(
        OffsetDateTime generatedAt,
        String disclaimer,
        String calculationConvention,
        RecipeInfo recipe,
        RuleInfo rule,
        List<NutrientBasis> nutrients) {

    public record RecipeInfo(
            String name,
            BigDecimal totalWeightG,
            BigDecimal servingSizeG) {
    }

    public record RuleInfo(String ruleSetCode, String ruleVersion) {
    }

    public record NutrientBasis(
            String code,
            String name,
            String displayUnit,
            boolean dataComplete,
            List<String> missingIngredients,
            /** Unrounded recipe total, in the nutrient's storage unit. */
            BigDecimal totalUnrounded,
            /** Rule parameters actually applied to produce the displays below. */
            RuleParams rule,
            ColumnBasis per100g,
            ColumnBasis perServing,
            List<ContributionBasis> contributions) {
    }

    public record RuleParams(
            String displayUnit,
            BigDecimal roundingIncrement,
            BigDecimal zeroThreshold,
            BigDecimal traceThreshold) {
    }

    public record ColumnBasis(
            BigDecimal unrounded,
            BigDecimal rounded,
            String display,
            String state) {
    }

    public record ContributionBasis(
            String ingredientName,
            BigDecimal amountG,
            BigDecimal valuePer100g,
            BigDecimal contributionToTotal) {
    }

    public static LabelExport from(LabelResult result) {
        List<NutrientBasis> nutrients = result.nutrients().stream()
                .map(LabelExport::toBasis)
                .toList();
        return new LabelExport(
                OffsetDateTime.now(),
                result.disclaimer(),
                "Totals are accumulated unrounded (BigDecimal). Per-100g and per-serving "
                        + "values are computed independently from the unrounded total and "
                        + "rounded to the rule increment (HALF_UP); rounded values are never "
                        + "used as input to further conversions. Unknown ingredient values are "
                        + "reported as data gaps, never as zero.",
                new RecipeInfo(result.recipeName(), result.totalWeightG(), result.servingSizeG()),
                new RuleInfo(result.ruleSetCode(), result.ruleVersion()),
                nutrients);
    }

    private static NutrientBasis toBasis(NutrientResult n) {
        return new NutrientBasis(
                n.code(),
                n.name(),
                n.displayUnit(),
                n.dataComplete(),
                n.missingIngredients(),
                n.totalUnrounded(),
                new RuleParams(n.rule().displayUnit(), n.rule().roundingIncrement(),
                        n.rule().zeroThreshold(), n.rule().traceThreshold()),
                new ColumnBasis(n.per100g().unrounded(), n.per100g().rounded(),
                        n.per100g().display(), n.per100g().state().name()),
                new ColumnBasis(n.perServing().unrounded(), n.perServing().rounded(),
                        n.perServing().display(), n.perServing().state().name()),
                n.contributions().stream()
                        .map(c -> new ContributionBasis(c.ingredientName(), c.amountG(),
                                c.valuePer100g(), c.contributionToTotal()))
                        .toList());
    }
}
