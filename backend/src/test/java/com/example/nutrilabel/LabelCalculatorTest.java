package com.example.nutrilabel;

import com.example.nutrilabel.calc.IngredientLine;
import com.example.nutrilabel.calc.LabelCalculator;
import com.example.nutrilabel.calc.LabelResult;
import com.example.nutrilabel.calc.NutrientDef;
import com.example.nutrilabel.calc.NutrientResult;
import com.example.nutrilabel.calc.RecipeInput;
import com.example.nutrilabel.calc.RuleDef;
import com.example.nutrilabel.calc.ValueState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Engine tests using the same parameters as the seeded SAMPLE rule set v1.0
 * plus the seeded boundary recipes.
 */
class LabelCalculatorTest {

    // ---- nutrient definitions (mirror V2__seed_rules.sql) ----
    private static final NutrientDef ENERGY_KCAL =
            new NutrientDef("energy_kcal", "能量", "kcal", 1, null);
    private static final NutrientDef ENERGY_KJ = new NutrientDef("energy_kj", "能量", "kJ", 2,
            new NutrientDef.Derivation("energy_kcal", new BigDecimal("4.184"), BigDecimal.ONE));
    private static final NutrientDef PROTEIN = new NutrientDef("protein", "蛋白质", "g", 3, null);
    private static final NutrientDef FAT = new NutrientDef("fat", "脂肪", "g", 4, null);
    private static final NutrientDef SODIUM = new NutrientDef("sodium", "钠", "mg", 7, null);
    private static final NutrientDef SALT = new NutrientDef("salt_eq", "盐当量", "g", 8,
            new NutrientDef.Derivation("sodium", new BigDecimal("2.5"), new BigDecimal("1000")));

    private static final List<NutrientDef> NUTRIENTS =
            List.of(ENERGY_KCAL, ENERGY_KJ, PROTEIN, FAT, SODIUM, SALT);

    private static Map<String, RuleDef> rulesV1() {
        Map<String, RuleDef> rules = new HashMap<>();
        rules.put("energy_kcal", new RuleDef("energy_kcal", "kcal",
                bd("1"), bd("0"), null));
        rules.put("energy_kj", new RuleDef("energy_kj", "kJ",
                bd("1"), bd("0"), null));
        rules.put("protein", new RuleDef("protein", "g",
                bd("0.1"), bd("0.05"), null));
        rules.put("fat", new RuleDef("fat", "g",
                bd("0.1"), bd("0.05"), null));
        rules.put("sodium", new RuleDef("sodium", "mg",
                bd("1"), bd("0.5"), bd("5")));
        rules.put("salt_eq", new RuleDef("salt_eq", "g",
                bd("0.01"), bd("0.005"), null));
        return rules;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static IngredientLine line(String name, String amountG, Map<String, String> values) {
        Map<String, BigDecimal> known = new HashMap<>();
        values.forEach((k, v) -> known.put(k, bd(v)));
        return new IngredientLine(name, bd(amountG), known);
    }

    private static LabelResult calculate(RecipeInput recipe) {
        return LabelCalculator.calculate(recipe, NUTRIENTS, rulesV1(),
                "SAMPLE-RULESET", "1.0", "sample disclaimer");
    }

    private static NutrientResult nutrient(LabelResult result, String code) {
        return result.nutrients().stream()
                .filter(n -> n.code().equals(code)).findFirst().orElseThrow();
    }

    /** Flour with the same values as the seeded 小麦粉. */
    private static Map<String, String> flour() {
        return Map.of("energy_kcal", "364", "protein", "10.3", "fat", "1.0", "sodium", "2");
    }

    @Test
    void roundingBoundaryHalfUp() {
        // 100 g flour only; serving 25 g.
        // protein per serving = 10.3 * 25/100 = 2.575  -> HALF_UP to 0.1 -> 2.6
        // fat     per serving = 1.0  * 25/100 = 0.25   -> HALF_UP to 0.1 -> 0.3
        LabelResult result = calculate(new RecipeInput("临界", bd("100"), bd("25"),
                List.of(line("小麦粉", "100", flour()))));

        assertEquals("2.575", nutrient(result, "protein").perServing().unrounded()
                .stripTrailingZeros().toPlainString());
        assertEquals("2.6", nutrient(result, "protein").perServing().display());
        assertEquals("0.3", nutrient(result, "fat").perServing().display());
        assertEquals("10.3", nutrient(result, "protein").per100g().display());
    }

    @Test
    void zeroThresholdBoundaryIsInclusive() {
        // sodium per serving = 2 mg * 25/100 = 0.5 mg, exactly the zero threshold:
        // not below zero threshold, but below trace threshold 5 -> "<5", not "0".
        LabelResult result = calculate(new RecipeInput("临界", bd("100"), bd("25"),
                List.of(line("小麦粉", "100", flour()))));

        assertEquals(ValueState.TRACE, nutrient(result, "sodium").perServing().state());
        assertEquals("<5", nutrient(result, "sodium").perServing().display());
    }

    @Test
    void per100gAndPerServingAreRoundedIndependently() {
        // salt_eq per 100 g = 2 mg * 2.5/1000 = 0.005 g  -> rounds to 0.01
        // salt_eq per serving (25 g) = 0.00125 g         -> below zero threshold -> "0"
        // If per-serving were derived from the ROUNDED per-100g value it would be
        // 0.01 * 25/100 = 0.0025 -> still "0", so also check a divergent protein case.
        LabelResult result = calculate(new RecipeInput("临界", bd("100"), bd("25"),
                List.of(line("小麦粉", "100", flour()))));

        assertEquals("0.01", nutrient(result, "salt_eq").per100g().display());
        assertEquals(ValueState.ZERO, nutrient(result, "salt_eq").perServing().state());
        assertEquals("0", nutrient(result, "salt_eq").perServing().display());
    }

    @Test
    void perServingNeverDerivesFromRoundedPer100g() {
        // Synthetic: protein 1.25 g/100 g, serving = 50 g (half the batch).
        // Correct: per serving = 0.625 -> "0.6".
        // Wrong (chained): rounded per100g 1.3 * 0.5 = 0.65 -> "0.7".
        LabelResult result = calculate(new RecipeInput("链式", bd("100"), bd("50"),
                List.of(line("合成原料", "100",
                        Map.of("energy_kcal", "100", "protein", "1.25",
                                "fat", "0", "sodium", "0")))));

        assertEquals("1.3", nutrient(result, "protein").per100g().display());
        assertEquals("0.6", nutrient(result, "protein").perServing().display());
    }

    @Test
    void gramMilligramConversionViaDerivation() {
        // 1.5 g salt (sodium 39000 mg/100 g) in a 401.5 g batch:
        // sodium total = 585 mg; salt_eq total = 585 * 2.5 / 1000 = 1.4625 g.
        LabelResult result = calculate(new RecipeInput("盐", bd("401.5"), bd("20"),
                List.of(line("食盐", "1.5",
                        Map.of("energy_kcal", "0", "protein", "0", "fat", "0",
                                "sodium", "39000")))));

        NutrientResult salt = nutrient(result, "salt_eq");
        assertEquals(0, bd("1.4625").compareTo(salt.totalUnrounded()));
        assertEquals("g", salt.displayUnit());
        // per 100 g = 1.4625/401.5*100 = 0.36425... -> 0.36
        assertEquals("0.36", salt.per100g().display());
    }

    @Test
    void kcalToKjConversion() {
        LabelResult result = calculate(new RecipeInput("能量", bd("100"), bd("25"),
                List.of(line("小麦粉", "100", flour()))));

        // 364 kcal * 4.184 = 1522.976 kJ -> 1523
        assertEquals("1523", nutrient(result, "energy_kj").per100g().display());
    }

    @Test
    void traceAmountShowsTraceMarker() {
        // Mirrors seeded 微量测试配方: starch 500 g, vanilla 6 g, baking powder 0.2 g.
        // sodium per 100 g = (0.54 + 21.2) / 506.2 * 100 = 4.2947... mg -> "<5"
        LabelResult result = calculate(new RecipeInput("微量", bd("506.2"), bd("25"),
                List.of(
                        line("玉米淀粉", "500",
                                Map.of("energy_kcal", "381", "protein", "0.3",
                                        "fat", "0.1", "sodium", "0")),
                        line("香草精", "6",
                                Map.of("energy_kcal", "288", "protein", "0.1",
                                        "fat", "0.1", "sodium", "9")),
                        line("泡打粉", "0.2",
                                Map.of("energy_kcal", "163", "protein", "0.1",
                                        "fat", "0.4", "sodium", "10600")))));

        NutrientResult sodium = nutrient(result, "sodium");
        assertEquals(ValueState.TRACE, sodium.per100g().state());
        assertEquals("<5", sodium.per100g().display());
        assertTrue(sodium.per100g().unrounded().compareTo(bd("4")) > 0);
        assertTrue(sodium.per100g().unrounded().compareTo(bd("5")) < 0);
    }

    @Test
    void unknownValueIsAGapNotAZero() {
        // Milk powder has no sodium value -> sodium and derived salt_eq are GAP,
        // while protein (known for all lines) stays OK.
        LabelResult result = calculate(new RecipeInput("缺口", bd("230"), bd("30"),
                List.of(
                        line("小麦粉", "150", flour()),
                        line("全脂奶粉", "50",
                                Map.of("energy_kcal", "496", "protein", "26.3", "fat", "26.7")),
                        line("白砂糖", "30",
                                Map.of("energy_kcal", "400", "protein", "0",
                                        "fat", "0", "sodium", "0")))));

        NutrientResult sodium = nutrient(result, "sodium");
        assertFalse(sodium.dataComplete());
        assertEquals(ValueState.GAP, sodium.per100g().state());
        assertEquals(ValueState.GAP, sodium.perServing().state());
        assertEquals("—", sodium.per100g().display());
        assertEquals(List.of("全脂奶粉"), sodium.missingIngredients());
        // partial total over known ingredients is kept for the audit trail:
        // flour 2 mg/100g * 150 g = 3 mg, sugar 0 -> 3 mg
        assertEquals(0, bd("3.0").compareTo(sodium.totalUnrounded()));

        NutrientResult salt = nutrient(result, "salt_eq");
        assertFalse(salt.dataComplete());
        assertEquals(ValueState.GAP, salt.per100g().state());

        NutrientResult protein = nutrient(result, "protein");
        assertTrue(protein.dataComplete());
        // (10.3*1.5 + 26.3*0.5 + 0) / 230 * 100 = 12.4347... -> 12.4
        assertEquals("12.4", protein.per100g().display());
    }

    @Test
    void knownZeroIsNotAGap() {
        // Corn starch sodium is known to be 0: no gap, and a real "0" display.
        LabelResult result = calculate(new RecipeInput("零", bd("100"), bd("25"),
                List.of(line("玉米淀粉", "100",
                        Map.of("energy_kcal", "381", "protein", "0.3",
                                "fat", "0.1", "sodium", "0")))));

        NutrientResult sodium = nutrient(result, "sodium");
        assertTrue(sodium.dataComplete());
        assertEquals(ValueState.ZERO, sodium.per100g().state());
        assertEquals("0", sodium.per100g().display());
    }
}
