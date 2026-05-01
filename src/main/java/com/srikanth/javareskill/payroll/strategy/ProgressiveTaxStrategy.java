package com.srikanth.javareskill.payroll.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A {@link TaxStrategy} that applies progressive (tiered/bracket-based) taxation.
 *
 * <p>Income is taxed at different rates for each bracket.  Only the portion of
 * income that falls within a bracket is taxed at that bracket's rate.</p>
 *
 * <h3>Default brackets (monthly, INR-oriented for illustration):</h3>
 * <pre>
 *  Bracket 1 :   0 –  20 000  →  0 %
 *  Bracket 2 :  20 001 –  50 000  → 10 %
 *  Bracket 3 :  50 001 – 100 000  → 20 %
 *  Bracket 4 : 100 001+           → 30 %
 * </pre>
 *
 * <p>Custom brackets can be supplied via the {@link #ProgressiveTaxStrategy(List)} constructor.</p>
 */
public final class ProgressiveTaxStrategy implements TaxStrategy {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Represents a single tax bracket.
     *
     * @param lowerBound inclusive lower bound (use {@code BigDecimal.ZERO} for the first bracket)
     * @param upperBound exclusive upper bound (use {@code null} for the top-most open-ended bracket)
     * @param rate       tax rate as a decimal fraction [0, 1]
     */
    public record Bracket(BigDecimal lowerBound, BigDecimal upperBound, BigDecimal rate) {

        public Bracket {
            Objects.requireNonNull(lowerBound, "lowerBound must not be null");
            Objects.requireNonNull(rate,       "rate must not be null");
            if (lowerBound.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("lowerBound must be >= 0");
            }
            if (upperBound != null && upperBound.compareTo(lowerBound) <= 0) {
                throw new IllegalArgumentException("upperBound must be > lowerBound");
            }
            if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("rate must be between 0 and 1");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Default brackets
    // -------------------------------------------------------------------------

    public static final List<Bracket> DEFAULT_BRACKETS = List.of(
            new Bracket(BigDecimal.ZERO,                    new BigDecimal("20000"),  new BigDecimal("0.00")),
            new Bracket(new BigDecimal("20000"),            new BigDecimal("50000"),  new BigDecimal("0.10")),
            new Bracket(new BigDecimal("50000"),            new BigDecimal("100000"), new BigDecimal("0.20")),
            new Bracket(new BigDecimal("100000"),           null,                     new BigDecimal("0.30"))
    );

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final List<Bracket> brackets;

    /** Creates a strategy using the {@link #DEFAULT_BRACKETS}. */
    public ProgressiveTaxStrategy() {
        this(DEFAULT_BRACKETS);
    }

    /**
     * Creates a strategy with custom brackets.
     *
     * <p>Brackets are sorted by {@code lowerBound} automatically; only one bracket
     * may have a {@code null} upperBound (the top bracket).</p>
     */
    public ProgressiveTaxStrategy(List<Bracket> brackets) {
        Objects.requireNonNull(brackets, "brackets must not be null");
        if (brackets.isEmpty()) {
            throw new IllegalArgumentException("at least one bracket is required");
        }
        List<Bracket> sorted = new ArrayList<>(brackets);
        sorted.sort(Comparator.comparing(Bracket::lowerBound));
        this.brackets = List.copyOf(sorted);
    }

    // -------------------------------------------------------------------------
    // Strategy implementation
    // -------------------------------------------------------------------------

    /**
     * Calculates tax by iterating over sorted brackets and applying each
     * bracket's rate to the portion of {@code grossSalary} that falls within it.
     *
     * <p>A <strong>switch expression</strong> determines the taxable amount
     * for each bracket based on whether the bracket has a finite upper bound
     * or is the open-ended top bracket ({@code upperBound == null}):</p>
     * <pre>{@code
     * BigDecimal taxableInBracket = switch (bracket.upperBound()) {
     *     case null         -> grossSalary.subtract(bracketStart);
     *     case BigDecimal u -> u.min(grossSalary).subtract(bracketStart).max(ZERO);
     * };
     * }</pre>
     *
     * <h3>Switch-expression features demonstrated</h3>
     * <ul>
     *   <li>{@code case null ->} — null-matching arm (JDK 21+ pattern matching for switch,
     *       but here we use a simple ternary equivalent for JDK 17 compatibility)</li>
     *   <li>Expression result assigned directly to a local variable</li>
     *   <li>Arrow syntax — no fall-through</li>
     * </ul>
     */
    @Override
    public BigDecimal calculateTax(BigDecimal grossSalary) {
        Objects.requireNonNull(grossSalary, "grossSalary must not be null");

        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal remaining = grossSalary;

        for (Bracket bracket : brackets) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal bracketStart = bracket.lowerBound();

            // Skip brackets whose lower bound is at or above the gross salary
            if (grossSalary.compareTo(bracketStart) <= 0) continue;

            // Switch expression: compute taxable portion based on bracket type
            BigDecimal taxableInBracket = switch (bracketKind(bracket)) {
                case OPEN_ENDED -> // Top bracket: no upper bound
                        grossSalary.subtract(bracketStart);
                case BOUNDED    -> // Regular bracket: cap at upper bound or salary
                        bracket.upperBound().min(grossSalary)
                                .subtract(bracketStart)
                                .max(BigDecimal.ZERO);
            };

            tax = tax.add(taxableInBracket.multiply(bracket.rate()));
            remaining = remaining.subtract(taxableInBracket);
        }

        return tax.setScale(SCALE, ROUNDING);
    }

    // -------------------------------------------------------------------------
    // Bracket classification for switch expression
    // -------------------------------------------------------------------------

    /**
     * Classifies a bracket as either {@code OPEN_ENDED} (no upper bound)
     * or {@code BOUNDED} (finite upper bound).  Used as the selector for the
     * switch expression inside {@link #calculateTax(BigDecimal)}.
     */
    private enum BracketKind { OPEN_ENDED, BOUNDED }

    private static BracketKind bracketKind(Bracket bracket) {
        return bracket.upperBound() == null ? BracketKind.OPEN_ENDED : BracketKind.BOUNDED;
    }

    @Override
    public String name() {
        return "Progressive(" + brackets.size() + " brackets)";
    }

    public List<Bracket> getBrackets() {
        return brackets;
    }

    @Override
    public String toString() {
        return name();
    }
}

