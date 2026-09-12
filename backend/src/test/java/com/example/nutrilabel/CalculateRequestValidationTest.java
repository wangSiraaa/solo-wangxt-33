package com.example.nutrilabel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bean-validation cascade on CalculateRequest: invalid scenarios must be
 * rejected with 4xx before any calculation happens.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CalculateRequestValidationTest {

    @Autowired
    private MockMvc mvc;

    private static String body(String scenario) {
        return "{\"recipeId\":1,\"scenarios\":[" + scenario + "]}";
    }

    @Test
    void zeroServingSizeIsRejected() throws Exception {
        mvc.perform(post("/api/labels/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"ruleVersionId\":1,\"servingSizeG\":0}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]")
                        .value(org.hamcrest.Matchers.containsString("servingSizeG")));
    }

    @Test
    void negativeServingSizeIsRejected() throws Exception {
        mvc.perform(post("/api/labels/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"ruleVersionId\":1,\"servingSizeG\":-20}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingRuleVersionIsRejected() throws Exception {
        mvc.perform(post("/api/labels/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"servingSizeG\":20}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyScenarioListIsRejected() throws Exception {
        mvc.perform(post("/api/labels/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipeId\":1,\"scenarios\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportAlsoValidatesScenarios() throws Exception {
        mvc.perform(post("/api/labels/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"ruleVersionId\":1,\"servingSizeG\":0}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void positiveServingSizeStillWorks() throws Exception {
        mvc.perform(post("/api/labels/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("{\"ruleVersionId\":1,\"servingSizeG\":25}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].servingSizeG").value(25));
    }
}
