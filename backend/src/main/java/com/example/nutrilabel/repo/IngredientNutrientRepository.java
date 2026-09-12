package com.example.nutrilabel.repo;

import com.example.nutrilabel.domain.IngredientNutrient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface IngredientNutrientRepository extends JpaRepository<IngredientNutrient, Long> {

    @Query("select in_ from IngredientNutrient in_ join fetch in_.nutrient "
            + "where in_.ingredient.id in :ingredientIds")
    List<IngredientNutrient> findByIngredientIdInWithNutrient(
            @Param("ingredientIds") Collection<Long> ingredientIds);
}
