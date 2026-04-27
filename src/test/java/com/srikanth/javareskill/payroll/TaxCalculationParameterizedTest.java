package com.srikanth.javareskill.payroll;

import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.payroll.strategy.ExemptTaxStrategy;
import com.srikanth.javareskill.payroll.strategy.FlatRateTaxStrategy;
import com.srikanth.javareskill.payroll.strategy.ProgressiveTaxStrategy;
import com.srikanth.javareskill.payroll.strategy.TaxStrategy;
import com.srikanth.javareskill.payroll.strategy.TaxStrategyFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Parameterized edge-case tests for the three {@link TaxStrategy} implementations
 * and the {@link TaxStrategyFactory}.
 *
 * <h2>JUnit 5 features demonstrated</h2>
 * <ul>
 *   <li>{@code @CsvSource}     – inline tabular input/expected pairs</li>
 *   <li>{@code @MethodSource}  – complex multi-argument cases via {@link Arguments}</li>
 *   <li>{@code @ValueSource}   – single-value sweep (e.g. boundary salaries)</li>
 *   <li>{@code @EnumSource}    – iterate over all enum constants</li>
 * </ul>
 */
@DisplayName("Tax Calculation – Parameterized Edge Cases")
class TaxCalculationParameterizedTest {

    // =========================================================================
    // FlatRateTaxStrategy
    // =========================================================================

    @Nested
    @DisplayName("FlatRateTaxStrategy")
    class FlatRateParameterizedTests {

        /**
         * Happy-path: rate × gross → expected tax (all values as strings for
         * exact {@link BigDecimal} comparison via {@code isEqualByComparingTo}).
         */
        @ParameterizedTest(name = "[{index}] rate={0}, gross={1} → tax={2}")
        @CsvSource({
                "0.10,  100000, 10000.00",   // 10% of 100 000
                "0.20,   50000, 10000.00",   // 20% of  50 000
                "0.30,   33333,  9999.90",   // 30% of  33 333 (no rounding needed)
                "0.15,   33333,  4999.95",   // 15% of  33 333 (scale check)
                "0.00,   99999,     0.00",   // zero rate → always zero
                "1.00,    1000,  1000.00",   // 100% rate → full amount
                "0.05,       1,     0.05",   // tiny salary
                "0.25, 1000000,250000.00",   // large salary
        })
        @DisplayName("calculates correct tax for rate × gross")
        void flatRate_correctTax(String rate, String gross, String expectedTax) {
            TaxStrategy strategy = new FlatRateTaxStrategy(new BigDecimal(rate));
            BigDecimal tax = strategy.calculateTax(new BigDecimal(gross));
            assertThat(tax).isEqualByComparingTo(new BigDecimal(expectedTax));
        }

        /** Result must always be rounded to exactly 2 decimal places. */
        @ParameterizedTest(name = "[{index}] gross={0}")
        @ValueSource(strings = {"1", "3", "7", "33333", "66667", "99999"})
        @DisplayName("result always has scale = 2 (HALF_UP rounding)")
        void flatRate_scaleIsAlwaysTwo(String gross) {
            TaxStrategy strategy = new FlatRateTaxStrategy(0.15); // produces many fractional digits
            BigDecimal tax = strategy.calculateTax(new BigDecimal(gross));
            assertThat(tax.scale())
                    .as("scale of tax for gross %s", gross)
                    .isEqualTo(2);
        }

        /** Tax must never exceed the gross salary (rate is capped at 1). */
        @ParameterizedTest(name = "[{index}] rate={0}")
        @ValueSource(doubles = {0.0, 0.10, 0.20, 0.30, 0.50, 1.0})
        @DisplayName("tax is never greater than gross salary")
        void flatRate_taxNeverExceedsGross(double rate) {
            BigDecimal gross = new BigDecimal("80000");
            TaxStrategy strategy = new FlatRateTaxStrategy(rate);
            assertThat(strategy.calculateTax(gross))
                    .isLessThanOrEqualTo(gross);
        }

        /** Constructor must reject any rate outside [0, 1]. */
        @ParameterizedTest(name = "[{index}] invalid rate={0}")
        @ValueSource(doubles = {-0.001, -1.0, 1.001, 2.0, Double.MAX_VALUE})
        @DisplayName("constructor rejects rate outside [0, 1]")
        void flatRate_invalidRateRejected(double invalidRate) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new FlatRateTaxStrategy(invalidRate));
        }

        /** name() must encode the percentage correctly for every valid rate. */
        @ParameterizedTest(name = "[{index}] rate={0} → name contains \"{1}%\"")
        @CsvSource({
                "0.10, 10",
                "0.20, 20",
                "0.30, 30",
                "0.50, 50",
                "1.00, 100",
                "0.00, 0",
        })
        @DisplayName("name() encodes the percentage")
        void flatRate_nameEncodesPercentage(String rate, String expectedPct) {
            String name = new FlatRateTaxStrategy(new BigDecimal(rate)).name();
            assertThat(name).contains(expectedPct + "%");
        }
    }

    // =========================================================================
    // ProgressiveTaxStrategy (default brackets)
    // =========================================================================

    @Nested
    @DisplayName("ProgressiveTaxStrategy – default brackets")
    class ProgressiveParameterizedTests {

        private static final TaxStrategy PROGRESSIVE = new ProgressiveTaxStrategy();

        /**
         * Bracket boundaries and representative mid-bracket values with their
         * expected total tax.
         *
         * Default brackets:
         *   0 –  20 000 @  0 %
         *  20 001 –  50 000 @ 10 %
         *  50 001 – 100 000 @ 20 %
         * 100 001+          @ 30 %
         */
        @ParameterizedTest(name = "[{index}] gross={0} → tax={1}")
        @CsvSource({
                // ── bracket 1 (0%) ──────────────────────────────────────────────
                "0,          0.00",   // zero salary
                "1,          0.00",   // $1 in zero bracket
                "10000,      0.00",   // mid first bracket
                "20000,      0.00",   // exactly at first bracket upper bound

                // ── bracket 2 (10%) ─────────────────────────────────────────────
                "20001,      0.10",   // just above first bracket boundary
                "25000,    500.00",   // mid second bracket: (25000-20000)*0.10
                "50000,   3000.00",   // exactly at second bracket upper bound

                // ── bracket 3 (20%) ─────────────────────────────────────────────
                "50001,   3000.20",   // just above second bracket boundary
                "75000,   8000.00",   // mid third bracket: 3000 + (75000-50000)*0.20
                "100000, 13000.00",   // exactly at third bracket upper bound

                // ── bracket 4 (30%) ─────────────────────────────────────────────
                "100001,  13000.30",  // just above third bracket boundary
                "150000,  28000.00",  // 0 + 3000 + 10000 + (150000-100000)*0.30
                "200000,  43000.00",  // 0 + 3000 + 10000 + (200000-100000)*0.30
        })
        @DisplayName("correct tax for gross salary across all brackets and boundaries")
        void progressive_correctTax(String gross, String expectedTax) {
            BigDecimal tax = PROGRESSIVE.calculateTax(new BigDecimal(gross));
            assertThat(tax)
                    .as("tax for gross %s", gross)
                    .isEqualByComparingTo(new BigDecimal(expectedTax));
        }

        /** Effective tax rate must always increase (or stay equal) as salary rises. */
        @ParameterizedTest(name = "[{index}] salary pair ({0}, {1})")
        @MethodSource("ascendingSalaryPairs")
        @DisplayName("effective rate is non-decreasing as gross salary rises")
        void progressive_effectiveRateNonDecreasing(String lowerGross, String higherGross) {
            BigDecimal lo = new BigDecimal(lowerGross);
            BigDecimal hi = new BigDecimal(higherGross);

            BigDecimal taxLo = PROGRESSIVE.calculateTax(lo);
            BigDecimal taxHi = PROGRESSIVE.calculateTax(hi);

            // effective rate = tax / gross (avoid divide-by-zero for salary = 0)
            if (lo.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal rateLo = taxLo.divide(lo, 10, java.math.RoundingMode.HALF_UP);
                BigDecimal rateHi = taxHi.divide(hi, 10, java.math.RoundingMode.HALF_UP);
                assertThat(rateHi)
                        .as("effective rate for %s should be >= rate for %s", higherGross, lowerGross)
                        .isGreaterThanOrEqualTo(rateLo);
            }
        }

        static Stream<Arguments> ascendingSalaryPairs() {
            return Stream.of(
                    Arguments.of("10000",  "20000"),
                    Arguments.of("20000",  "50000"),
                    Arguments.of("50000", "100000"),
                    Arguments.of("100000","200000"),
                    Arguments.of("10000", "200000")
            );
        }

        /** Tax + net = gross must always hold (invariant). */
        @ParameterizedTest(name = "[{index}] gross={0}")
        @ValueSource(strings = {"0", "1", "20000", "50000", "100000", "150000", "500000"})
        @DisplayName("tax + net == gross (invariant)")
        void progressive_netPlusTaxEqualsGross(String gross) {
            BigDecimal grossBD = new BigDecimal(gross);
            BigDecimal tax     = PROGRESSIVE.calculateTax(grossBD);
            BigDecimal net     = grossBD.subtract(tax);
            assertThat(net.add(tax))
                    .isEqualByComparingTo(grossBD);
        }

        /** Bracket-boundary salaries: tax must be non-negative. */
        @ParameterizedTest(name = "[{index}] gross={0}")
        @ValueSource(strings = {"0", "19999", "20000", "20001", "49999", "50000", "50001", "99999", "100000", "100001"})
        @DisplayName("tax is never negative at bracket boundaries")
        void progressive_taxNonNegativeAtBoundaries(String gross) {
            BigDecimal tax = PROGRESSIVE.calculateTax(new BigDecimal(gross));
            assertThat(tax)
                    .as("tax for gross %s must be >= 0", gross)
                    .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }
    }

    // =========================================================================
    // ProgressiveTaxStrategy – custom brackets
    // =========================================================================

    @Nested
    @DisplayName("ProgressiveTaxStrategy – custom brackets")
    class ProgressiveCustomBracketsTests {

        /**
         * Two-bracket scheme: 5% up to 10 000, then 25% above.
         * Tests several input/output pairs using a shared factory method.
         */
        @ParameterizedTest(name = "[{index}] gross={0} → tax={1}")
        @MethodSource("customBracketCases")
        @DisplayName("custom two-bracket scheme produces correct tax")
        void customBrackets_correctTax(String gross, String expectedTax) {
            List<ProgressiveTaxStrategy.Bracket> brackets = List.of(
                    new ProgressiveTaxStrategy.Bracket(
                            BigDecimal.ZERO, new BigDecimal("10000"), new BigDecimal("0.05")),
                    new ProgressiveTaxStrategy.Bracket(
                            new BigDecimal("10000"), null, new BigDecimal("0.25"))
            );
            TaxStrategy strategy = new ProgressiveTaxStrategy(brackets);
            assertThat(strategy.calculateTax(new BigDecimal(gross)))
                    .isEqualByComparingTo(new BigDecimal(expectedTax));
        }

        static Stream<Arguments> customBracketCases() {
            return Stream.of(
                    // ── within first bracket (5%) ──
                    Arguments.of("0",     "0.00"),    // zero salary
                    Arguments.of("5000",  "250.00"),  // 5000 * 5%
                    Arguments.of("10000", "500.00"),  // 10000 * 5%
                    // ── spanning both brackets ──
                    Arguments.of("10001", "500.25"),  // 500 + 1 * 25%
                    Arguments.of("20000", "3000.00"), // 500 + 10000 * 25%
                    Arguments.of("50000","13500.00")  // 500 + 40000 * 25%
            );
        }

        /** A single catch-all bracket (flat-equivalent via progressive). */
        @ParameterizedTest(name = "[{index}] gross={0} → tax={1}")
        @CsvSource({
                "0,      0.00",
                "10000,  2000.00",
                "50000, 10000.00",
        })
        @DisplayName("single open-ended bracket behaves like a flat rate")
        void singleBracket_behavesLikeFlat(String gross, String expectedTax) {
            List<ProgressiveTaxStrategy.Bracket> singleBracket = List.of(
                    new ProgressiveTaxStrategy.Bracket(BigDecimal.ZERO, null, new BigDecimal("0.20"))
            );
            TaxStrategy strategy = new ProgressiveTaxStrategy(singleBracket);
            assertThat(strategy.calculateTax(new BigDecimal(gross)))
                    .isEqualByComparingTo(new BigDecimal(expectedTax));
        }
    }

    // =========================================================================
    // ExemptTaxStrategy
    // =========================================================================

    @Nested
    @DisplayName("ExemptTaxStrategy")
    class ExemptParameterizedTests {

        /** Zero tax regardless of salary — tested across a wide range. */
        @ParameterizedTest(name = "[{index}] gross={0} → tax=0.00")
        @ValueSource(strings = {"0", "1", "999", "20000", "100000", "1000000", "99999999"})
        @DisplayName("always returns zero tax for any gross salary")
        void exempt_alwaysZero(String gross) {
            BigDecimal tax = ExemptTaxStrategy.INSTANCE.calculateTax(new BigDecimal(gross));
            assertThat(tax).isEqualByComparingTo("0.00");
        }

        /** ExemptTaxStrategy must produce less-or-equal tax than any flat rate. */
        @ParameterizedTest(name = "[{index}] rate={0}")
        @ValueSource(doubles = {0.01, 0.10, 0.20, 0.30, 1.0})
        @DisplayName("exempt strategy tax is always <= flat-rate tax for same gross")
        void exempt_lessThanOrEqualToFlat(double rate) {
            BigDecimal gross = new BigDecimal("75000");
            BigDecimal exemptTax = ExemptTaxStrategy.INSTANCE.calculateTax(gross);
            BigDecimal flatTax   = new FlatRateTaxStrategy(rate).calculateTax(gross);
            assertThat(exemptTax).isLessThanOrEqualTo(flatTax);
        }
    }

    // =========================================================================
    // TaxStrategyFactory – role mapping
    // =========================================================================

    @Nested
    @DisplayName("TaxStrategyFactory")
    class FactoryParameterizedTests {

        /** Every enum constant must resolve to a non-null strategy without throwing. */
        @ParameterizedTest(name = "[{index}] role={0}")
        @EnumSource(Role.class)
        @DisplayName("every Role resolves to a non-null TaxStrategy")
        void factory_everyRoleHasStrategy(Role role) {
            assertThatNoException()
                    .isThrownBy(() -> {
                        TaxStrategy strategy = TaxStrategyFactory.forRole(role);
                        assertThat(strategy).isNotNull();
                    });
        }

        /** Roles that should map to ProgressiveTaxStrategy. */
        @ParameterizedTest(name = "[{index}] role={0} → ProgressiveTaxStrategy")
        @EnumSource(value = Role.class, names = {"DIRECTOR", "SENIOR_MANAGER"})
        @DisplayName("DIRECTOR and SENIOR_MANAGER use ProgressiveTaxStrategy")
        void factory_progressiveRoles(Role role) {
            assertThat(TaxStrategyFactory.forRole(role))
                    .isInstanceOf(ProgressiveTaxStrategy.class);
        }

        /** Roles that should map to FlatRateTaxStrategy at 20%. */
        @ParameterizedTest(name = "[{index}] role={0} → FlatRate(20%)")
        @EnumSource(value = Role.class, names = {"MANAGER", "SENIOR_ENGINEER"})
        @DisplayName("MANAGER and SENIOR_ENGINEER use FlatRateTaxStrategy(20%)")
        void factory_flatTwentyRoles(Role role) {
            TaxStrategy strategy = TaxStrategyFactory.forRole(role);
            assertThat(strategy).isInstanceOf(FlatRateTaxStrategy.class);
            assertThat(((FlatRateTaxStrategy) strategy).getRate())
                    .isEqualByComparingTo("0.20");
        }

        /** Roles that should map to FlatRateTaxStrategy at 10%. */
        @ParameterizedTest(name = "[{index}] role={0} → FlatRate(10%)")
        @EnumSource(value = Role.class, names = {"ENGINEER", "ANALYST", "HR"})
        @DisplayName("ENGINEER, ANALYST, and HR use FlatRateTaxStrategy(10%)")
        void factory_flatTenRoles(Role role) {
            TaxStrategy strategy = TaxStrategyFactory.forRole(role);
            assertThat(strategy).isInstanceOf(FlatRateTaxStrategy.class);
            assertThat(((FlatRateTaxStrategy) strategy).getRate())
                    .isEqualByComparingTo("0.10");
        }

        /**
         * Strategy correctness end-to-end: given a role and a gross salary,
         * the factory-resolved strategy must produce the expected tax.
         */
        @ParameterizedTest(name = "[{index}] role={0}, gross={1} → tax={2}")
        @MethodSource("roleAndSalaryCases")
        @DisplayName("factory-resolved strategy produces expected tax per role")
        void factory_endToEndTax(Role role, String gross, String expectedTax) {
            TaxStrategy strategy = TaxStrategyFactory.forRole(role);
            BigDecimal tax = strategy.calculateTax(new BigDecimal(gross));
            assertThat(tax)
                    .as("tax for role=%s gross=%s", role, gross)
                    .isEqualByComparingTo(new BigDecimal(expectedTax));
        }

        static Stream<Arguments> roleAndSalaryCases() {
            return Stream.of(
                    // ENGINEER / ANALYST / HR → 10%
                    Arguments.of(Role.ENGINEER,        "50000",  "5000.00"),
                    Arguments.of(Role.ANALYST,         "40000",  "4000.00"),
                    Arguments.of(Role.HR,              "30000",  "3000.00"),
                    // MANAGER / SENIOR_ENGINEER → 20%
                    Arguments.of(Role.MANAGER,         "60000", "12000.00"),
                    Arguments.of(Role.SENIOR_ENGINEER, "80000", "16000.00"),
                    // DIRECTOR / SENIOR_MANAGER → progressive
                    // 0-20000@0% + 20000-50000@10% + 50000-100000@20% + 100000-150000@30%
                    // = 0 + 3000 + 10000 + 15000 = 28000
                    Arguments.of(Role.DIRECTOR,        "150000", "28000.00"),
                    Arguments.of(Role.SENIOR_MANAGER,  "150000", "28000.00")
            );
        }
    }
}

