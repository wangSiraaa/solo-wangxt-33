package com.example.nutrilabel.repo;

import com.example.nutrilabel.domain.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

    @Query("select ri from RecipeIngredient ri join fetch ri.ingredient where ri.recipe.id = :recipeId")
    List<RecipeIngredient> findByRecipeIdWithIngredient(@Param("recipeId") Long recipeId);
}
