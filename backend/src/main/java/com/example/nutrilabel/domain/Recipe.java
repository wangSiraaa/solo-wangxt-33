package com.example.nutrilabel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "recipe")
public class Recipe {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "default_serving_size_g", nullable = false, precision = 19, scale = 6)
    private BigDecimal defaultServingSizeG;

    /**
     * Weight of the finished product (after cooking loss etc.). When null,
     * the sum of ingredient amounts is used as the recipe weight.
     */
    @Column(name = "yield_weight_g", precision = 19, scale = 6)
    private BigDecimal yieldWeightG;

    protected Recipe() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getDefaultServingSizeG() {
        return defaultServingSizeG;
    }

    public BigDecimal getYieldWeightG() {
        return yieldWeightG;
    }
}
