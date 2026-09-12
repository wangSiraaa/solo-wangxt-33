package com.example.nutrilabel.repo;

import com.example.nutrilabel.domain.Nutrient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NutrientRepository extends JpaRepository<Nutrient, Long> {

    List<Nutrient> findAllByOrderByDisplayOrder();
}
