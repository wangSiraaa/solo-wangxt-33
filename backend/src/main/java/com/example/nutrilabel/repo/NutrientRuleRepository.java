package com.example.nutrilabel.repo;

import com.example.nutrilabel.domain.NutrientRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NutrientRuleRepository extends JpaRepository<NutrientRule, Long> {

    @Query("select r from NutrientRule r join fetch r.nutrient where r.ruleVersion.id = :versionId")
    List<NutrientRule> findByRuleVersionIdWithNutrient(@Param("versionId") Long ruleVersionId);
}
