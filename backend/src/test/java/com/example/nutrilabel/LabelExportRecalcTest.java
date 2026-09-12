package com.example.nutrilabel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that the export payload carries every rule parameter needed to
 * independently reproduce the displayed values: for each nutrient column the
 * test recomputes the display string from the exported unrounded value and
 * the exported rule parameters, and requires an exact match.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LabelExportRecalcTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void exportedValuesReproduceDisplay_boundaryRecipeV1() throws Exception {
        // recipe 3 (舍入临界配方): exercises OK / TRACE / ZERO and the 2.575 half-up case
        JsonNode root = export(3, 1, null);
        assertAllColumnsReproducible(root);
    }

    @Test
    void exportedValuesReproduceDisplay_gapRecipe() throws Exception {
        // recipe 4 (数据缺口配方): GAP columns must export "—" and list the gap
        JsonNode root = export(4, 1, "30");
        assertAllColumnsReproducible(root);
        JsonNode sodium = nutrient(root, "sodium");
        assertEquals("GAP", sodium.at("/per100g/state").asText());
        assertEquals("—", sodium.at("/per100g/display").asText());
    }

    @Test
    void exportedValuesReproduceDisplay_cookiesV11() throws Exception {
        // recipe 1 with rule version 2 (v1.1): coarser increments must be the
        // ones exported (energy increment 5, sodium increment 5 / trace 10)
        JsonNode root = export(1, 2, "20");
        assertAllColumnsReproducible(root);
        JsonNode energyRule = nutrient(root, "energy_kcal").get("rule");
        assertEquals(0, new BigDecimal("5").compareTo(energyRule.get("roundingIncrement").decimalValue()));
        JsonNode sodiumRule = nutrient(root, "sodium").get("rule");
        assertEquals(0, new BigDecimal("5").compareTo(sodiumRule.get("roundingIncrement").decimalValue()));
        assertEquals(0, new BigDecimal("0.5").compareTo(sodiumRule.get("zeroThreshold").decimalValue()));
        assertEquals(0, new BigDecimal("10").compareTo(sodiumRule.get("traceThreshold").decimalValue()));
    }

    @Test
    void exportedRuleParamsMatchSeedV1() throws Exception {
        JsonNode root = export(3, 1, null);
        JsonNode sodiumRule = nutrient(root, "sodium").get("rule");
        assertEquals("mg", sodiumRule.get("displayUnit").asText());
        assertEquals(0, new BigDecimal("1").compareTo(sodiumRule.get("roundingIncrement").decimalValue()));
        assertEquals(0, new BigDecimal("0.5").compareTo(sodiumRule.get("zeroThreshold").decimalValue()));
        assertEquals(0, new BigDecimal("5").compareTo(sodiumRule.get("traceThreshold").decimalValue()));
        // nutrient-level displayUnit and rule.displayUnit must agree
        JsonNode sodium = nutrient(root, "sodium");
        assertEquals(sodium.get("displayUnit").asText(), sodiumRule.get("displayUnit").asText());
    }

    // ---- helpers ----

    private JsonNode export(long recipeId, long ruleVersionId, String servingSizeG) throws Exception {
        String scenario = servingSizeG == null
                ? "{\"ruleVersionId\":" + ruleVersionId + "}"
                : "{\"ruleVersionId\":" + ruleVersionId + ",\"servingSizeG\":" + servingSizeG + "}";
        MvcResult result = mvc.perform(post("/api/labels/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipeId\":" + recipeId + ",\"scenarios\":[" + scenario + "]}"))
                .andExpect(status().isOk())
                .andReturn();
        return om.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static JsonNode nutrient(JsonNode root, String code) {
        for (JsonNode n : root.get("nutrients")) {
            if (n.get("code").asText().equals(code)) {
                return n;
            }
        }
        throw new AssertionError("nutrient not found in export: " + code);
    }

    private static void assertAllColumnsReproducible(JsonNode root) {
        assertTrue(root.get("nutrients").size() > 0, "export has nutrients");
        for (JsonNode n : root.get("nutrients")) {
            JsonNode rule = n.get("rule");
            for (String column : new String[]{"per100g", "perServing"}) {
                JsonNode col = n.get(column);
                String expected = recomputeDisplay(
                        col.get("unrounded").decimalValue(), rule, col.get("state").asText());
                String context = n.get("code").asText() + " " + column
                        + " (unrounded=" + col.get("unrounded").asText() + ")";
                assertEquals(expected, col.get("display").asText(),
                    "display not reproducible from exported basis: " + context);
                if ("OK".equals(col.get("state").asText())) {
                    // rounded field must equal increment rounding of unrounded
                    BigDecimal rounded = roundToIncrement(
                            col.get("unrounded").decimalValue(),
                            rule.get("roundingIncrement").decimalValue());
                    assertEquals(0, rounded.compareTo(col.get("rounded").decimalValue()),
                            "rounded mismatch: " + context);
                }
            }
        }
    }

    /** Independent re-implementation of the display semantics (mirrors the spec). */
    private static String recomputeDisplay(BigDecimal unrounded, JsonNode rule, String state) {
        if ("GAP".equals(state)) {
            return "—";
        }
        if (unrounded.compareTo(rule.get("zeroThreshold").decimalValue()) < 0) {
            return "0";
        }
        JsonNode trace = rule.get("traceThreshold");
        if (trace != null && !trace.isNull()
                && unrounded.compareTo(trace.decimalValue()) < 0) {
            return "<" + fmt(trace.decimalValue());
        }
        return fmt(roundToIncrement(unrounded, rule.get("roundingIncrement").decimalValue()));
    }

    private static BigDecimal roundToIncrement(BigDecimal value, BigDecimal increment) {
        return value.divide(increment, 0, RoundingMode.HALF_UP).multiply(increment);
    }

    private static String fmt(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
