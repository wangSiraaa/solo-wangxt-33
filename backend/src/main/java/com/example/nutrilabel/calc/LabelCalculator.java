package com.example.nutrilabel.calc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nutrition label calculation engine.
 *
 * Guarantees:
 * <ul>
 *   <li>All arithmetic uses BigDecimal; totals are kept unrounded.</li>
 *   <li>The per-100-g and per-serving columns are each computed from the
 *       unrounded recipe total and rounded independently. A rounded value is
 *       never used as the input of another conversion.</li>
 *   <li>Unknown ingredient values produce a data gap (state GAP); they are
 *       never silently treated as zero.</li>
 *   <li>Derived nutrients (e.g. salt equivalent) are computed from unrounded
 *       totals of their source nutrient.</li>
 * </ul>
 */
public final class LabelCalculator {

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private LabelCalculator() {
    }

    public static LabelResult calculate(RecipeInput recipe,
                                        List<NutrientDef> nutrients,
                                        Map<String, RuleDef> rulesByNutrient,
                                        String ruleSetCode,
                                        String ruleVersion,
                                        String disclaimer) {
        if (recipe.totalWeightG() == null || recipe.totalWeightG().signum() <= 0) {
            throw new IllegalArgumentException("Recipe total weight must be positive");
        }

        List<NutrientDef> ordered = nutrients.stream()
                .sorted(Comparator.comparingInt(NutrientDef::displayOrder))
                .toList();

        // code -> accumulated total (storage unit, unrounded) + gap bookkeeping
        Map<String, Accumulation> totals = new LinkedHashMap<>();

        // Pass 1: stored nutrients.
        for (NutrientDef nutrient : ordered) {
            if (!nutrient.isDerived()) {
                totals.put(nutrient.code(), accumulate(recipe.lines(), nutrient.code()));
            }
        }
        // Pass 2: derived nutrients, from the UNROUNDED totals of pass 1.
        for (NutrientDef nutrient : ordered) {
            if (nutrient.isDerived()) {
                totals.put(nutrient.code(), derive(totals, nutrient));
            }
        }

        List<NutrientResult> results = new ArrayList<>();
        for (NutrientDef nutrient : ordered) {
            RuleDef rule = rulesByNutrient.get(nutrient.code());
            if (rule == null) {
                throw new IllegalStateException(
                        "No rule for nutrient " + nutrient.code() + " in this rule version");
            }
            Accumulation acc = totals.get(nutrient.code());

            // Convert the unrounded total to the display unit once, then derive
            // both columns from that unrounded figure.
            BigDecimal totalDisplay = UnitConverter.convert(
                    acc.total(), nutrient.storageUnit(), rule.displayUnit());
            BigDecimal per100 = totalDisplay
                    .divide(recipe.totalWeightG(), MC)
                    .multiply(HUNDRED);
            BigDecimal perServing = totalDisplay
                    .divide(recipe.totalWeightG(), MC)
                    .multiply(recipe.servingSizeG());

            results.add(new NutrientResult(
                    nutrient.code(),
                    nutrient.name(),
                    rule.displayUnit(),
                    acc.complete(),
                    acc.total(),
                    toColumn(per100, rule, acc.complete()),
                    toColumn(perServing, rule, acc.complete()),
                    acc.missingIngredients(),
                    acc.contributions()));
        }

        return new LabelResult(
                recipe.recipeName(),
                recipe.totalWeightG(),
                recipe.servingSizeG(),
                ruleSetCode,
                ruleVersion,
                disclaimer,
                results);
    }

    /** Sum contributions over all lines; unknown values open a gap, never a zero. */
    private static Accumulation accumulate(List<IngredientLine> lines, String nutrientCode) {
        BigDecimal total = BigDecimal.ZERO;
        List<String> missing = new ArrayList<>();
        List<Contribution> contributions = new ArrayList<>();
        for (IngredientLine line : lines) {
            BigDecimal per100g = line.knownValuesPer100g().get(nutrientCode);
            if (per100g == null) {
                missing.add(line.ingredientName());
                contributions.add(new Contribution(
                        line.ingredientName(), line.amountG(), null, null));
                continue;
            }
            BigDecimal contribution = per100g.multiply(line.amountG()).divide(HUNDRED, MC);
            total = total.add(contribution);
            contributions.add(new Contribution(
                    line.ingredientName(), line.amountG(), per100g, contribution));
        }
        return new Accumulation(total, missing.isEmpty(), missing, contributions);
    }

    /** Derive a nutrient from the unrounded total of its source nutrient. */
    private static Accumulation derive(Map<String, Accumulation> totals, NutrientDef nutrient) {
        NutrientDef.Derivation d = nutrient.derivation();
        Accumulation source = totals.get(d.fromNutrientCode());
        if (source == null) {
            throw new IllegalStateException(
                    "Derived nutrient " + nutrient.code() + " references unknown source "
                            + d.fromNutrientCode());
        }
        BigDecimal total = source.total()
                .multiply(d.multiplier())
                .divide(d.divisor(), MC);
        List<Contribution> contributions = source.contributions().stream()
                .map(c -> new Contribution(
                        c.ingredientName(),
                        c.amountG(),
                        c.valuePer100g() == null ? null
                                : c.valuePer100g().multiply(d.multiplier()).divide(d.divisor(), MC),
                        c.contributionToTotal() == null ? null
                                : c.contributionToTotal().multiply(d.multiplier()).divide(d.divisor(), MC)))
                .toList();
        return new Accumulation(total, source.complete(), source.missingIngredients(), contributions);
    }

    /** Apply zero/trace/rounding rules to one unrounded column value. */
    private static ColumnValue toColumn(BigDecimal unrounded, RuleDef rule, boolean dataComplete) {
        if (!dataComplete) {
            return new ColumnValue(unrounded, null, "—", ValueState.GAP);
        }
        if (unrounded.compareTo(rule.zeroThreshold()) < 0) {
            return new ColumnValue(unrounded, BigDecimal.ZERO, "0", ValueState.ZERO);
        }
        if (rule.traceThreshold() != null && unrounded.compareTo(rule.traceThreshold()) < 0) {
            return new ColumnValue(unrounded, null,
                    "<" + format(rule.traceThreshold()), ValueState.TRACE);
        }
        BigDecimal rounded = roundToIncrement(unrounded, rule.roundingIncrement());
        return new ColumnValue(unrounded, rounded, format(rounded), ValueState.OK);
    }

    /** Round to the nearest multiple of {@code increment}, ties away from zero. */
    static BigDecimal roundToIncrement(BigDecimal value, BigDecimal increment) {
        return value.divide(increment, 0, RoundingMode.HALF_UP).multiply(increment);
    }

    /** Render a BigDecimal without scientific notation or trailing zeros. */
    static String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private record Accumulation(BigDecimal total,
                                boolean complete,
                                List<String> missingIngredients,
                                List<Contribution> contributions) {
    }
}
