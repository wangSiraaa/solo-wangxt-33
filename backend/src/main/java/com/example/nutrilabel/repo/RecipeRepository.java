package com.example.nutrilabel.repo;

import com.example.nutrilabel.domain.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
}
