package com.srikanth.javareskill.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A permanent (full-time) employee with benefits.
 *
 * <p>{@code final} — this is a leaf in the sealed hierarchy and cannot
 * be extended further.</p>
 *
 * <h2>Type-specific fields</h2>
 * <ul>
 *   <li>{@code benefitsPercentage} – the percentage of base salary added as
 *       benefits (e.g. health insurance, retirement contribution).
 *       Expressed as a decimal fraction: {@code 0.15} = 15 %.</li>
 *   <li>{@code annualBonus} – a fixed annual bonus amount.</li>
 * </ul>
 */
public final class PermanentEmployee extends EmployeeType {

    private final BigDecimal benefitsPercentage;
    private final BigDecimal annualBonus;

    /**
     * @param employee            the underlying employee; must not be {@code null}
     * @param benefitsPercentage  fraction [0, 1] representing the benefits rate;
     *                            must not be {@code null}
     * @param annualBonus         fixed annual bonus; must not be {@code null},
     *                            must be ≥ 0
     */
    public PermanentEmployee(Employee employee,
                              BigDecimal benefitsPercentage,
                              BigDecimal annualBonus) {
        super(employee);
        this.benefitsPercentage = Objects.requireNonNull(benefitsPercentage,
                "benefitsPercentage must not be null");
        this.annualBonus = Objects.requireNonNull(annualBonus,
                "annualBonus must not be null");

        if (benefitsPercentage.compareTo(BigDecimal.ZERO) < 0
                || benefitsPercentage.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    "benefitsPercentage must be between 0 and 1, got: " + benefitsPercentage);
        }
        if (annualBonus.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("annualBonus must not be negative");
        }
    }

    public BigDecimal getBenefitsPercentage() { return benefitsPercentage; }
    public BigDecimal getAnnualBonus()        { return annualBonus; }

    /**
     * Annual compensation = (12 × salary) + (12 × salary × benefitsPercentage) + annualBonus.
     *
     * <p>Result is rounded to 2 decimal places (HALF_UP).</p>
     */
    @Override
    public BigDecimal annualCompensation() {
        BigDecimal annualSalary  = getSalary().multiply(BigDecimal.valueOf(12));
        BigDecimal benefitsValue = annualSalary.multiply(benefitsPercentage);
        return annualSalary.add(benefitsValue).add(annualBonus)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String typeName() {
        return "Permanent";
    }

    @Override
    public String toString() {
        return "PermanentEmployee{employee=" + getEmployee()
                + ", benefitsPercentage=" + benefitsPercentage
                + ", annualBonus=" + annualBonus + "}";
    }
}

