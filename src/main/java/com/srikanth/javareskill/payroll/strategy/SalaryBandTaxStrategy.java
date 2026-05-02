package com.srikanth.javareskill.payroll.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A {@link TaxStrategy} that selects a single flat tax rate based on which
 * <em>salary band</em> the gross salary falls into and applies that rate to
 * the <strong>entire</strong> gross salary.
 *
 * <h2>Tax bands (defaults)</h2>
 * <pre>
 * ┌────────────────────────────┬──────────┐
 * │ Gross salary               │ Tax rate │
 * ├────────────────────────────┼──────────┤
 * │ < 50 000                   │   10 %   │
 * │ 50 000 – 100 000 inclusive │   20 %   │
 * │ > 100 000                  │   30 %   │
 * └────────────────────────────┴──────────┘
 * </pre>
 *
 * <h2>Difference from {@link ProgressiveTaxStrategy}</h2>
 * <p>{@code ProgressiveTaxStrategy} taxes each <em>slice</em> of income at its
 * bracket's rate (marginal taxation).  {@code SalaryBandTaxStrategy} looks at the
 * total salary, determines which band it belongs to, and applies <em>one flat
 * rate to the whole amount</em> — a simpler payroll-band model common in many
 * SME HR systems.</p>
 *
 * <h2>Customisation</h2>
 * <p>The three boundaries and rates can be overridden via the full constructor,
 * making the strategy easily re-usable for different payroll rules without
 * subclassing (OCP – Open/Closed Principle).</p>
 *
 * <h2>Precision</h2>
 * <p>All arithmetic uses {@link RoundingMode#HALF_UP} with 2 decimal places.</p>
 */
public final class SalaryBandTaxStrategy implements TaxStrategy {

    private static final int          SCALE    = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    // ── Default band boundaries ───────────────────────────────────────────────

    /** Upper boundary (exclusive) of the low band. */
    public static final BigDecimal DEFAULT_LOW_THRESHOLD  = new BigDecimal("50000");

    /** Upper boundary (inclusive) of the mid band. */
    public static final BigDecimal DEFAULT_HIGH_THRESHOLD = new BigDecimal("100000");

    // ── Default rates ─────────────────────────────────────────────────────────

    /** Tax rate for salaries below {@code lowThreshold}  (10 %). */
    public static final BigDecimal DEFAULT_LOW_RATE  = new BigDecimal("0.10");

    /** Tax rate for salaries in {@code [lowThreshold, highThreshold]}  (20 %). */
    public static final BigDecimal DEFAULT_MID_RATE  = new BigDecimal("0.20");

    /** Tax rate for salaries above {@code highThreshold}  (30 %). */
    public static final BigDecimal DEFAULT_HIGH_RATE = new BigDecimal("0.30");

    // ── Instance fields ───────────────────────────────────────────────────────

    /**
     * Upper boundary (exclusive) of the low band.
     * Salary {@code < lowThreshold} is taxed at {@code lowRate}.
     */
    private final BigDecimal lowThreshold;

    /**
     * Upper boundary (inclusive) of the mid band.
     * Salary {@code >= lowThreshold && <= highThreshold} is taxed at {@code midRate}.
     * Salary {@code > highThreshold} is taxed at {@code highRate}.
     */
    private final BigDecimal highThreshold;

    private final BigDecimal lowRate;
    private final BigDecimal midRate;
    private final BigDecimal highRate;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Creates a strategy with the default bands:
     * {@code < 50 000 → 10 %}, {@code 50 000–100 000 → 20 %}, {@code > 100 000 → 30 %}.
     */
    public SalaryBandTaxStrategy() {
        this(DEFAULT_LOW_THRESHOLD, DEFAULT_HIGH_THRESHOLD,
             DEFAULT_LOW_RATE, DEFAULT_MID_RATE, DEFAULT_HIGH_RATE);
    }

    /**
     * Creates a strategy with custom band boundaries and rates.
     *
     * @param lowThreshold  upper boundary (exclusive) of the low band; must be &gt; 0
     * @param highThreshold upper boundary (inclusive) of the mid band; must be &gt; {@code lowThreshold}
     * @param lowRate       flat rate applied when salary &lt; lowThreshold; [0, 1]
     * @param midRate       flat rate applied when lowThreshold ≤ salary ≤ highThreshold; [0, 1]
     * @param highRate      flat rate applied when salary &gt; highThreshold; [0, 1]
     */
    public SalaryBandTaxStrategy(BigDecimal lowThreshold,  BigDecimal highThreshold,
                                  BigDecimal lowRate, BigDecimal midRate, BigDecimal highRate) {
        Objects.requireNonNull(lowThreshold,  "lowThreshold must not be null");
        Objects.requireNonNull(highThreshold, "highThreshold must not be null");
        Objects.requireNonNull(lowRate,       "lowRate must not be null");
        Objects.requireNonNull(midRate,       "midRate must not be null");
        Objects.requireNonNull(highRate,      "highRate must not be null");

        if (lowThreshold.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("lowThreshold must be > 0, got: " + lowThreshold);
        }
        if (highThreshold.compareTo(lowThreshold) <= 0) {
            throw new IllegalArgumentException(
                    "highThreshold must be > lowThreshold; got high=" + highThreshold
                    + ", low=" + lowThreshold);
        }
        validateRate("lowRate",  lowRate);
        validateRate("midRate",  midRate);
        validateRate("highRate", highRate);

        this.lowThreshold  = lowThreshold;
        this.highThreshold = highThreshold;
        this.lowRate       = lowRate;
        this.midRate       = midRate;
        this.highRate      = highRate;
    }

    // ── TaxStrategy implementation ────────────────────────────────────────────

    /**
     * Determines the applicable band via a {@code switch} expression and
     * multiplies the entire gross salary by the band's flat rate.
     *
     * <h3>Band selection logic</h3>
     * <pre>
     * grossSalary &lt; lowThreshold                      → LOW  band → lowRate
     * grossSalary &gt;= lowThreshold &amp;&amp; &lt;= highThreshold → MID  band → midRate
     * grossSalary &gt; highThreshold                     → HIGH band → highRate
     * </pre>
     *
     * @param grossSalary the employee's gross monthly salary; must be ≥ 0
     * @return the tax amount (grossSalary × applicable rate), rounded to 2 d.p.
     */
    @Override
    public BigDecimal calculateTax(BigDecimal grossSalary) {
        Objects.requireNonNull(grossSalary, "grossSalary must not be null");
        if (grossSalary.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("grossSalary must not be negative");
        }

        // Switch expression — band is resolved here; rate applied below
        Band band = switch (resolveBand(grossSalary)) {
            case LOW  -> Band.LOW;
            case MID  -> Band.MID;
            case HIGH -> Band.HIGH;
        };

        BigDecimal rate = switch (band) {
            case LOW  -> lowRate;
            case MID  -> midRate;
            case HIGH -> highRate;
        };

        return grossSalary.multiply(rate).setScale(SCALE, ROUNDING);
    }

    /**
     * Returns the human-readable name including the configured thresholds and rates.
     *
     * <p>Example: {@code SalaryBand(<50000→10%, 50000-100000→20%, >100000→30%)}</p>
     */
    @Override
    public String name() {
        return "SalaryBand(<%s→%.0f%%, %s-%s→%.0f%%, >%s→%.0f%%)".formatted(
                lowThreshold,
                lowRate.multiply(BigDecimal.valueOf(100)),
                lowThreshold, highThreshold,
                midRate.multiply(BigDecimal.valueOf(100)),
                highThreshold,
                highRate.multiply(BigDecimal.valueOf(100)));
    }

    @Override
    public String toString() { return name(); }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public BigDecimal getLowThreshold()  { return lowThreshold; }
    public BigDecimal getHighThreshold() { return highThreshold; }
    public BigDecimal getLowRate()       { return lowRate; }
    public BigDecimal getMidRate()       { return midRate; }
    public BigDecimal getHighRate()      { return highRate; }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private enum Band { LOW, MID, HIGH }

    /**
     * Resolves the salary band without branching on {@code if/else}.
     * The result feeds the switch expression in {@link #calculateTax}.
     */
    private Band resolveBand(BigDecimal grossSalary) {
        if (grossSalary.compareTo(lowThreshold) < 0)       return Band.LOW;
        if (grossSalary.compareTo(highThreshold) <= 0)     return Band.MID;
        return Band.HIGH;
    }

    private static void validateRate(String name, BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(name + " must be in [0, 1], got: " + rate);
        }
    }
}

