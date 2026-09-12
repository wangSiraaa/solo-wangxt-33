package com.example.nutrilabel.calc;

/** Display state of one nutrient column value. */
public enum ValueState {
    /** Rounded value is shown. */
    OK,
    /** Unrounded value fell below the zero threshold; "0" is shown. */
    ZERO,
    /** Unrounded value fell in the trace band; "&lt;threshold" is shown. */
    TRACE,
    /** At least one ingredient value is unknown; no number is shown. */
    GAP
}
