package com.example.nutrilabel.calc;

import java.math.BigDecimal;

/**
 * One column (per 100 g or per serving) of a nutrient row.
 *
 * @param unrounded full-precision value in the display unit; for a GAP column
 *                  this is the partial total over known ingredients only and
 *                  must be labelled as such wherever it is shown
 * @param rounded   rounded value, null when not applicable (GAP)
 * @param display   ready-to-render string ("0", "&lt;x", rounded number, or "—")
 */
public record ColumnValue(
        BigDecimal unrounded,
        BigDecimal rounded,
        String display,
        ValueState state) {
}
