package com.example.nutrilabel.dto;

import java.util.List;

public record RuleSetDto(
        Long id,
        String code,
        String name,
        String disclaimer,
        List<RuleVersionDto> versions) {

    public record RuleVersionDto(Long id, String version, String note) {
    }
}
