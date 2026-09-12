package com.example.nutrilabel.web;

import com.example.nutrilabel.calc.LabelResult;
import com.example.nutrilabel.dto.CalculateRequest;
import com.example.nutrilabel.dto.LabelExport;
import com.example.nutrilabel.service.LabelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    /** Calculate one or more scenarios (rule version x serving size) for a recipe. */
    @PostMapping("/calculate")
    public List<LabelResult> calculate(@Valid @RequestBody CalculateRequest request) {
        return labelService.calculate(request.recipeId(), toScenarios(request));
    }

    /**
     * Export a single scenario as a JSON document including the full
     * calculation basis (unrounded totals, per-ingredient contributions and
     * the rule parameters that produced every displayed number).
     */
    @PostMapping("/export")
    public ResponseEntity<LabelExport> export(@Valid @RequestBody CalculateRequest request) {
        if (request.scenarios().size() != 1) {
            throw new IllegalArgumentException("Export requires exactly one scenario");
        }
        LabelResult result = labelService.calculate(request.recipeId(), toScenarios(request))
                .get(0);
        String filename = "label-recipe" + request.recipeId()
                + "-rule" + result.ruleVersion() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(LabelExport.from(result));
    }

    private static List<LabelService.Scenario> toScenarios(CalculateRequest request) {
        return request.scenarios().stream()
                .map(s -> new LabelService.Scenario(s.ruleVersionId(), s.servingSizeG()))
                .toList();
    }
}
