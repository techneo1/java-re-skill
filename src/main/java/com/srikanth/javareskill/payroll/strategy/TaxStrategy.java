package com.srikanth.javareskill.payroll.strategy;

import java.math.BigDecimal;

/**
 * Strategy interface for tax calculation.
 *
 * <p>Implementations encapsulate a specific tax-calculation algorithm so that
 * the {@code PayrollService} remains open for extension (new strategies) but
 * closed for modification (Strategy Pattern – GoF).</p>
 *
 * <p>Contract:
 * <ul>
 *   <li>The returned value must be ≥ 0.</li>
 *   <li>The returned value must be ≤ {@code grossSalary}.</li>
 * </ul>
 * </p>
 */
public interface TaxStrategy {

    /**
     * Calculates the tax amount for the given gross salary.
     *
     * @param grossSalary the employee's gross monthly salary; never {@code null} and never negative
     * @return the tax amount to deduct; never {@code null}, never negative, never greater than
     *         {@code grossSalary}
     */
    BigDecimal calculateTax(BigDecimal grossSalary);

    /**
     * Human-readable name of the strategy (used in payroll reports and logs).
     */
    String name();
}

