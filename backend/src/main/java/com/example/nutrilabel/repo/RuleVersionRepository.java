package com.example.nutrilabel.repo;

import com.example.nutrilabel.domain.RuleVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleVersionRepository extends JpaRepository<RuleVersion, Long> {

    List<RuleVersion> findByRuleSetIdOrderByVersion(Long ruleSetId);
}
