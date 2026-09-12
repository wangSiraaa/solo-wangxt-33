package com.example.nutrilabel.web;

import com.example.nutrilabel.domain.RuleSet;
import com.example.nutrilabel.dto.RuleSetDto;
import com.example.nutrilabel.repo.RuleSetRepository;
import com.example.nutrilabel.repo.RuleVersionRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rulesets")
public class RuleSetController {

    private final RuleSetRepository ruleSetRepository;
    private final RuleVersionRepository ruleVersionRepository;

    public RuleSetController(RuleSetRepository ruleSetRepository,
                             RuleVersionRepository ruleVersionRepository) {
        this.ruleSetRepository = ruleSetRepository;
        this.ruleVersionRepository = ruleVersionRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<RuleSetDto> list() {
        return ruleSetRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    private RuleSetDto toDto(RuleSet ruleSet) {
        List<RuleSetDto.RuleVersionDto> versions = ruleVersionRepository
                .findByRuleSetIdOrderByVersion(ruleSet.getId()).stream()
                .map(v -> new RuleSetDto.RuleVersionDto(v.getId(), v.getVersion(), v.getNote()))
                .toList();
        return new RuleSetDto(ruleSet.getId(), ruleSet.getCode(), ruleSet.getName(),
                ruleSet.getDisclaimer(), versions);
    }
}
