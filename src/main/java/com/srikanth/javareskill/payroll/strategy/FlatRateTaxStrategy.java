package com.srikanth.javareskill.payroll.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A {@link TaxStrategy} that applies a single flat percentage to the entire gross salary.
 *
 * <p>Example: {@code rate = 0.20} means 20 % tax on every rupee earned.</p>
 */
public final class FlatRateTaxStrategy implements TaxStrategy {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final BigDecimal rate;   // 0.0 – 1.0

    /**
     * @param rate tax rate as a decimal fraction, e.g. {@code 0.20} for 20 %;
     *             must be in the range [0, 1]
     */
    public FlatRateTaxStrategy(BigDecimal rate) {
        Objects.requireNonNull(rate, "rate must not be null");
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("rate must be between 0 and 1, got: " + rate);
        }
        this.rate = rate;
    }

    /** Convenience constructor that accepts a {@code double}. */
    public FlatRateTaxStrategy(double rate) {
        this(BigDecimal.valueOf(rate));
    }

    @Override
    public BigDecimal calculateTax(BigDecimal grossSalary) {
        Objects.requireNonNull(grossSalary, "grossSalary must not be null");
        return grossSalary.multiply(rate).setScale(SCALE, ROUNDING);
    }

    @Override
    public String name() {
        return String.format("FlatRate(%.0f%%)", rate.multiply(BigDecimal.valueOf(100)));
    }

    public BigDecimal getRate() {
        return rate;
    }

    @Override
    public String toString() {
        return name();
    }
}

