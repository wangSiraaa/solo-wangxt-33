package com.example.nutrilabel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A named collection of labelling rules. The project ships a SAMPLE rule set
 * for R&amp;D trial calculations only; it does not claim compliance with any
 * national regulation.
 */
@Entity
@Table(name = "rule_set")
public class RuleSet {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String disclaimer;

    protected RuleSet() {
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

    public String getDisclaimer() {
        return disclaimer;
    }
}
