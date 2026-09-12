package com.example.nutrilabel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * An immutable, versioned snapshot of a rule set. Labels are always computed
 * against an explicit rule version so results stay reproducible and comparable.
 */
@Entity
@Table(name = "rule_version")
public class RuleVersion {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_set_id")
    private RuleSet ruleSet;

    @Column(nullable = false)
    private String version;

    @Column(length = 1000)
    private String note;

    protected RuleVersion() {
    }

    public Long getId() {
        return id;
    }

    public RuleSet getRuleSet() {
        return ruleSet;
    }

    public String getVersion() {
        return version;
    }

    public String getNote() {
        return note;
    }
}
