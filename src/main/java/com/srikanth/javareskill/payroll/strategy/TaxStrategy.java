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
 *
 * <h2>SOLID principles applied</h2>
 * <ul>
 *   <li><b>O – Open/Closed</b>: New tax behaviours are added by implementing
 *       this interface — existing strategies and the payroll service are never
 *       modified.</li>
 *   <li><b>L – Liskov Substitution</b>: Every concrete strategy
 *       ({@code FlatRateTaxStrategy}, {@code ProgressiveTaxStrategy},
 *       {@code ExemptTaxStrategy}) honours the contract above, making them
 *       fully interchangeable wherever a {@code TaxStrategy} is expected.</li>
 *   <li><b>I – Interface Segregation</b>: This interface declares exactly two
 *       methods that are always needed together.  Clients are not burdened with
 *       unrelated methods.</li>
 *   <li><b>D – Dependency Inversion</b>: {@code PayrollServiceImpl} depends on
 *       this abstraction, not on any concrete strategy class.</li>
 * </ul>
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

