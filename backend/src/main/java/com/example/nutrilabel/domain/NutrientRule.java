package com.example.nutrilabel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Display/rounding rule for one nutrient within one rule version.
 *
 * <ul>
 *   <li>values &lt; zeroThreshold display as "0"</li>
 *   <li>values &lt; traceThreshold (but not zero) display as "&lt;threshold"</li>
 *   <li>otherwise the value is rounded to the nearest multiple of
 *       roundingIncrement using HALF_UP</li>
 * </ul>
 * All comparisons happen against the unrounded value in the display unit.
 */
@Entity
@Table(name = "nutrient_rule")
public class NutrientRule {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_version_id")
    private RuleVersion ruleVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutrient_id")
    private Nutrient nutrient;

    @Column(name = "display_unit", nullable = false)
    private String displayUnit;

    @Column(name = "rounding_increment", nullable = false, precision = 19, scale = 6)
    private BigDecimal roundingIncrement;

    @Column(name = "zero_threshold", nullable = false, precision = 19, scale = 6)
    private BigDecimal zeroThreshold;

    @Column(name = "trace_threshold", precision = 19, scale = 6)
    private BigDecimal traceThreshold;

    protected NutrientRule() {
    }

    public Long getId() {
        return id;
    }

    public Nutrient getNutrient() {
        return nutrient;
    }

    public String getDisplayUnit() {
        return displayUnit;
    }

    public BigDecimal getRoundingIncrement() {
        return roundingIncrement;
    }

    public BigDecimal getZeroThreshold() {
        return zeroThreshold;
    }

    public BigDecimal getTraceThreshold() {
        return traceThreshold;
    }
}
