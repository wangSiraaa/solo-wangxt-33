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
 * Nutrient content of one ingredient, per 100 g, in the nutrient's storage
 * unit. A NULL value means "unknown" — it is a data gap and must never be
 * treated as zero.
 */
@Entity
@Table(name = "ingredient_nutrient")
public class IngredientNutrient {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutrient_id")
    private Nutrient nutrient;

    /** Value per 100 g; null = unknown (data gap). */
    @Column(name = "value_per_100g", precision = 19, scale = 6)
    private BigDecimal valuePer100g;

    protected IngredientNutrient() {
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public Nutrient getNutrient() {
        return nutrient;
    }

    public BigDecimal getValuePer100g() {
        return valuePer100g;
    }
}
