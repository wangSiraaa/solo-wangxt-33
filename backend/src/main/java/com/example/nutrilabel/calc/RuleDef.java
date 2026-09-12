package com.example.nutrilabel.calc;

import java.math.BigDecimal;

/**
 * Display rule for one nutrient within one rule version.
 *
 * @param zeroThreshold  unrounded display-unit values strictly below this show as "0"
 * @param traceThreshold unrounded display-unit values strictly below this (and not
 *                       zero) show as "&lt;threshold"; may be null (no trace band)
 */
public record RuleDef(
        String nutrientCode,
        String displayUnit,
        BigDecimal roundingIncrement,
        BigDecimal zeroThreshold,
        BigDecimal traceThreshold) {
}
