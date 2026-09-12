package com.example.nutrilabel.service;

import com.example.nutrilabel.calc.IngredientLine;
import com.example.nutrilabel.calc.LabelCalculator;
import com.example.nutrilabel.calc.LabelResult;
import com.example.nutrilabel.calc.NutrientDef;
import com.example.nutrilabel.calc.RecipeInput;
import com.example.nutrilabel.calc.RuleDef;
import com.example.nutrilabel.domain.IngredientNutrient;
import com.example.nutrilabel.domain.Nutrient;
import com.example.nutrilabel.domain.NutrientRule;
import com.example.nutrilabel.domain.Recipe;
import com.example.nutrilabel.domain.RecipeIngredient;
import com.example.nutrilabel.domain.RuleVersion;
import com.example.nutrilabel.repo.IngredientNutrientRepository;
import com.example.nutrilabel.repo.NutrientRepository;
import com.example.nutrilabel.repo.NutrientRuleRepository;
import com.example.nutrilabel.repo.RecipeIngredientRepository;
import com.example.nutrilabel.repo.RecipeRepository;
import com.example.nutrilabel.repo.RuleVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads persisted data, feeds the pure {@link LabelCalculator}, and returns
 * label results. One call may compute several scenarios (different rule
 * versions and/or serving sizes) for side-by-side comparison.
 */
@Service
public class LabelService {

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final IngredientNutrientRepository ingredientNutrientRepository;
    private final NutrientRepository nutrientRepository;
    private final NutrientRuleRepository nutrientRuleRepository;
    private final RuleVersionRepository ruleVersionRepository;

    public LabelService(RecipeRepository recipeRepository,
                        RecipeIngredientRepository recipeIngredientRepository,
                        IngredientNutrientRepository ingredientNutrientRepository,
                        NutrientRepository nutrientRepository,
                        NutrientRuleRepository nutrientRuleRepository,
                        RuleVersionRepository ruleVersionRepository) {
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.ingredientNutrientRepository = ingredientNutrientRepository;
        this.nutrientRepository = nutrientRepository;
        this.nutrientRuleRepository = nutrientRuleRepository;
        this.ruleVersionRepository = ruleVersionRepository;
    }

    public record Scenario(Long ruleVersionId, BigDecimal servingSizeG) {
    }

    @Transactional(readOnly = true)
    public List<LabelResult> calculate(Long recipeId, List<Scenario> scenarios) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown recipe id " + recipeId));
        List<RecipeIngredient> lines = recipeIngredientRepository.findByRecipeIdWithIngredient(recipeId);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Recipe " + recipeId + " has no ingredients");
        }

        List<Long> ingredientIds = lines.stream().map(l -> l.getIngredient().getId()).toList();
        // ingredientId -> nutrientCode -> known value per 100 g (nulls excluded on purpose)
        Map<Long, Map<String, BigDecimal>> knownValues = new HashMap<>();
        for (IngredientNutrient in : ingredientNutrientRepository
                .findByIngredientIdInWithNutrient(ingredientIds)) {
            if (in.getValuePer100g() == null) {
                continue; // unknown value: a data gap, not a zero
            }
            knownValues.computeIfAbsent(in.getIngredient().getId(), k -> new HashMap<>())
                    .put(in.getNutrient().getCode(), in.getValuePer100g());
        }

        BigDecimal totalWeight = recipe.getYieldWeightG() != null
                ? recipe.getYieldWeightG()
                : lines.stream()
                        .map(RecipeIngredient::getAmountG)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<NutrientDef> nutrientDefs = nutrientRepository.findAllByOrderByDisplayOrder().stream()
                .map(LabelService::toDef)
                .toList();

        List<LabelResult> results = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            RuleVersion version = ruleVersionRepository.findById(scenario.ruleVersionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown rule version id " + scenario.ruleVersionId()));
            Map<String, RuleDef> rules = new HashMap<>();
            for (NutrientRule r : nutrientRuleRepository.findByRuleVersionIdWithNutrient(version.getId())) {
                rules.put(r.getNutrient().getCode(), new RuleDef(
                        r.getNutrient().getCode(),
                        r.getDisplayUnit(),
                        r.getRoundingIncrement(),
                        r.getZeroThreshold(),
                        r.getTraceThreshold()));
            }

            BigDecimal serving = scenario.servingSizeG() != null
                    ? scenario.servingSizeG()
                    : recipe.getDefaultServingSizeG();

            List<IngredientLine> calcLines = lines.stream()
                    .map(l -> new IngredientLine(
                            l.getIngredient().getName(),
                            l.getAmountG(),
                            knownValues.getOrDefault(l.getIngredient().getId(), Map.of())))
                    .toList();

            results.add(LabelCalculator.calculate(
                    new RecipeInput(recipe.getName(), totalWeight, serving, calcLines),
                    nutrientDefs,
                    rules,
                    version.getRuleSet().getCode(),
                    version.getVersion(),
                    version.getRuleSet().getDisclaimer()));
        }
        return results;
    }

    private static NutrientDef toDef(Nutrient n) {
        NutrientDef.Derivation derivation = n.isDerived()
                ? new NutrientDef.Derivation(
                        n.getDerivedFromCode(), n.getDeriveMultiplier(), n.getDeriveDivisor())
                : null;
        return new NutrientDef(n.getCode(), n.getName(), n.getStorageUnit(),
                n.getDisplayOrder(), derivation);
    }
}
