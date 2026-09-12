package com.example.nutrilabel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke test over the seeded database (H2): migrations, endpoints and the
 * key calculation semantics end to end.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LabelApiSmokeTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void listsSeededData() throws Exception {
        mvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get("/api/rulesets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SAMPLE-RULESET"))
                .andExpect(jsonPath("$[0].versions.length()").value(2));
    }

    @Test
    void calculatesBoundaryRecipe() throws Exception {
        // recipe 3 (舍入临界配方), rule version 1 (v1.0), default serving 25 g
        mvc.perform(post("/api/labels/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipeId\":3,\"scenarios\":[{\"ruleVersionId\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nutrients[?(@.code=='protein')].perServing.display")
                        .value("2.6"))
                .andExpect(jsonPath("$[0].nutrients[?(@.code=='sodium')].perServing.display")
                        .value("<5"))
                .andExpect(jsonPath("$[0].nutrients[?(@.code=='salt_eq')].per100g.display")
                        .value("0.01"))
                .andExpect(jsonPath("$[0].nutrients[?(@.code=='salt_eq')].perServing.display")
                        .value("0"));
    }

    @Test
    void gapRecipeReportsGapNotZero() throws Exception {
        // recipe 4 (数据缺口配方): milk powder sodium unknown
        mvc.perform(post("/api/labels/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipeId\":4,\"scenarios\":[{\"ruleVersionId\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nutrients[?(@.code=='sodium')].per100g.state")
                        .value("GAP"))
                .andExpect(jsonPath("$[0].nutrients[?(@.code=='sodium')].per100g.display")
                        .value("—"))
                .andExpect(jsonPath("$[0].nutrients[?(@.code=='sodium')].missingIngredients[0]")
                        .value(org.hamcrest.Matchers.hasItem("全脂奶粉")))
                .andExpect(jsonPath("$[0].nutrients[?(@.code=='protein')].per100g.display")
                        .value("12.4"));
    }

    @Test
    void ruleVersionsDiffer() throws Exception {
        // recipe 1 cookies: energy per 100 g is 502.2 kcal -> v1.0 "502", v1.1 "500"
        mvc.perform(post("/api/labels/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipeId\":1,\"scenarios\":"
                                + "[{\"ruleVersionId\":1},{\"ruleVersionId\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nutrients[?(@.code=='energy_kcal')].per100g.display")
                        .value("502"))
                .andExpect(jsonPath("$[1].nutrients[?(@.code=='energy_kcal')].per100g.display")
                        .value("500"));
    }

    @Test
    void exportContainsCalculationBasis() throws Exception {
        mvc.perform(post("/api/labels/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipeId\":1,\"scenarios\":[{\"ruleVersionId\":1,\"servingSizeG\":20}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculationConvention").exists())
                .andExpect(jsonPath("$.disclaimer").exists())
                .andExpect(jsonPath("$.nutrients[?(@.code=='sodium')].contributions[0].length()")
                        .value(4));
    }
}
