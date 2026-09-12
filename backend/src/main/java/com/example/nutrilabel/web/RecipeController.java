package com.example.nutrilabel.web;

import com.example.nutrilabel.domain.IngredientNutrient;
import com.example.nutrilabel.domain.Nutrient;
import com.example.nutrilabel.domain.Recipe;
import com.example.nutrilabel.domain.RecipeIngredient;
import com.example.nutrilabel.dto.RecipeDetail;
import com.example.nutrilabel.dto.RecipeSummary;
import com.example.nutrilabel.repo.IngredientNutrientRepository;
import com.example.nutrilabel.repo.NutrientRepository;
import com.example.nutrilabel.repo.RecipeIngredientRepository;
import com.example.nutrilabel.repo.RecipeRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final IngredientNutrientRepository ingredientNutrientRepository;
    private final NutrientRepository nutrientRepository;

    public RecipeController(RecipeRepository recipeRepository,
                            RecipeIngredientRepository recipeIngredientRepository,
                            IngredientNutrientRepository ingredientNutrientRepository,
                            NutrientRepository nutrientRepository) {
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.ingredientNutrientRepository = ingredientNutrientRepository;
        this.nutrientRepository = nutrientRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<RecipeSummary> list() {
        return recipeRepository.findAll().stream()
                .map(r -> new RecipeSummary(r.getId(), r.getName(), r.getDescription(),
                        r.getDefaultServingSizeG(), totalWeight(r)))
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public RecipeDetail detail(@PathVariable Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown recipe id " + id));
        List<RecipeIngredient> lines = recipeIngredientRepository.findByRecipeIdWithIngredient(id);

        List<Long> ingredientIds = lines.stream().map(l -> l.getIngredient().getId()).toList();
        // ingredientId -> set of nutrient codes with KNOWN values
        Map<Long, Set<String>> known = new HashMap<>();
        for (IngredientNutrient in : ingredientNutrientRepository
                .findByIngredientIdInWithNutrient(ingredientIds)) {
            if (in.getValuePer100g() != null) {
                known.computeIfAbsent(in.getIngredient().getId(), k -> new HashSet<>())
                        .add(in.getNutrient().getCode());
            }
        }
        List<String> storedNutrientNames = nutrientRepository.findAllByOrderByDisplayOrder()
                .stream().filter(n -> !n.isDerived()).map(Nutrient::getName).toList();
        List<String> storedNutrientCodes = nutrientRepository.findAllByOrderByDisplayOrder()
                .stream().filter(n -> !n.isDerived()).map(Nutrient::getCode).toList();

        List<RecipeDetail.IngredientLineDto> ingredients = lines.stream()
                .map(l -> {
                    Set<String> knownCodes = known.getOrDefault(l.getIngredient().getId(), Set.of());
                    List<String> unknown = new java.util.ArrayList<>();
                    for (int i = 0; i < storedNutrientCodes.size(); i++) {
                        if (!knownCodes.contains(storedNutrientCodes.get(i))) {
                            unknown.add(storedNutrientNames.get(i));
                        }
                    }
                    return new RecipeDetail.IngredientLineDto(
                            l.getIngredient().getName(), l.getAmountG(), unknown);
                })
                .toList();

        return new RecipeDetail(recipe.getId(), recipe.getName(), recipe.getDescription(),
                recipe.getDefaultServingSizeG(), totalWeight(recipe), ingredients);
    }

    private BigDecimal totalWeight(Recipe recipe) {
        if (recipe.getYieldWeightG() != null) {
            return recipe.getYieldWeightG();
        }
        return recipeIngredientRepository.findByRecipeIdWithIngredient(recipe.getId()).stream()
                .map(RecipeIngredient::getAmountG)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
