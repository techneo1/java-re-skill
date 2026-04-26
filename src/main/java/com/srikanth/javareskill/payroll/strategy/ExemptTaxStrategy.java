package com.srikanth.javareskill.payroll.strategy;

import java.math.BigDecimal;

/**
 * A {@link TaxStrategy} that applies zero tax.
 *
 * <p>Suitable for tax-exempt employees (interns, certain contractual arrangements,
 * or employees in a tax-holiday period).</p>
 */
public final class ExemptTaxStrategy implements TaxStrategy {

    /** Singleton – this strategy carries no state. */
    public static final ExemptTaxStrategy INSTANCE = new ExemptTaxStrategy();

    private ExemptTaxStrategy() {}

    @Override
    public BigDecimal calculateTax(BigDecimal grossSalary) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public String name() {
        return "Exempt(0%)";
    }

    @Override
    public String toString() {
        return name();
    }
}

