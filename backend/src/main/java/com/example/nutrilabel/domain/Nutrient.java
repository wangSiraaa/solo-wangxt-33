package com.example.nutrilabel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A nutrient definition. Values are stored per 100 g of ingredient in
 * {@link #storageUnit}. A nutrient may instead be <em>derived</em> from another
 * nutrient (e.g. salt equivalent = sodium x 2.5 / 1000), in which case the
 * derivation is applied to the unrounded totals, never to rounded values.
 */
@Entity
@Table(name = "nutrient")
public class Nutrient {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    /** Unit of stored values, and the unit a derived value is expressed in. */
    @Column(name = "storage_unit", nullable = false)
    private String storageUnit;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** Code of the source nutrient when this nutrient is derived; null otherwise. */
    @Column(name = "derived_from_code")
    private String derivedFromCode;

    @Column(name = "derive_multiplier", precision = 19, scale = 6)
    private BigDecimal deriveMultiplier;

    @Column(name = "derive_divisor", precision = 19, scale = 6)
    private BigDecimal deriveDivisor;

    protected Nutrient() {
    }

    public boolean isDerived() {
        return derivedFromCode != null;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getStorageUnit() {
        return storageUnit;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public String getDerivedFromCode() {
        return derivedFromCode;
    }

    public BigDecimal getDeriveMultiplier() {
        return deriveMultiplier;
    }

    public BigDecimal getDeriveDivisor() {
        return deriveDivisor;
    }
}
