package com.srikanth.javareskill.payroll.strategy;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.payroll.impl.PayrollServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link SalaryBandTaxStrategy} and its integration with
 * {@link TaxStrategyFactory} and {@link PayrollServiceImpl}.
 *
 * <h2>Specification</h2>
 * <pre>
 * Salary &lt; 50 000              → 10 % tax on full salary
 * Salary 50 000 – 100 000 incl → 20 % tax on full salary
 * Salary &gt; 100 000             → 30 % tax on full salary
 * </pre>
 *
 * <h2>Test structure</h2>
 * <ol>
 *   <li><b>Unit</b>  — {@code SalaryBandTaxStrategy.calculateTax()} directly</li>
 *   <li><b>Boundary</b> — values at and around every band threshold</li>
 *   <li><b>Factory</b>  — {@code TaxStrategyFactory.forRole()} returns {@code SalaryBandTaxStrategy}</li>
 *   <li><b>Integration</b> — full payroll record produced by {@code PayrollServiceImpl}</li>
 *   <li><b>All roles</b>  — every {@link Role} gets salary-band tax</li>
 *   <li><b>Custom bands</b> — constructor override works correctly</li>
 *   <li><b>Validation</b>  — illegal arguments rejected</li>
 * </ol>
 */
@DisplayName("SalaryBandTaxStrategy")
class SalaryBandTaxStrategyTest {

    private SalaryBandTaxStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SalaryBandTaxStrategy();   // default bands
    }

    // =========================================================================
    // 1. Core band calculations
    // =========================================================================

    @Nested
    @DisplayName("Low band (salary < 50 000) → 10%")
    class LowBand {

        @ParameterizedTest(name = "salary={0} → tax={1}")
        @CsvSource({
            "0.00,        0.00",      // zero salary → zero tax
            "1.00,        0.10",      // smallest non-zero
            "10000.00,    1000.00",   // 10 % of 10 000
            "30000.00,    3000.00",   // 10 % of 30 000
            "49999.99,    5000.00",   // just below threshold (rounded HALF_UP)
        })
        @DisplayName("parameterised low-band salaries")
        void lowBand_tenPercent(String salary, String expectedTax) {
            BigDecimal tax = strategy.calculateTax(new BigDecimal(salary));
            assertThat(tax).isEqualByComparingTo(new BigDecimal(expectedTax));
        }

        @Test
        @DisplayName("49 999.99 → 4 999.999 rounds to 5 000.00 (HALF_UP)")
        void boundaryRounding() {
            // 49 999.99 × 0.10 = 4 999.999  →  rounds to 5 000.00
            assertThat(strategy.calculateTax(new BigDecimal("49999.99")))
                    .isEqualByComparingTo("5000.00");
        }
    }

    @Nested
    @DisplayName("Mid band (50 000 ≤ salary ≤ 100 000) → 20%")
    class MidBand {

        @ParameterizedTest(name = "salary={0} → tax={1}")
        @CsvSource({
            "50000.00,   10000.00",   // exact lower boundary → 20 %
            "75000.00,   15000.00",   // midpoint
            "99999.99,   20000.00",   // just below upper boundary (rounded)
            "100000.00,  20000.00",   // exact upper boundary (inclusive) → 20 %
        })
        @DisplayName("parameterised mid-band salaries")
        void midBand_twentyPercent(String salary, String expectedTax) {
            BigDecimal tax = strategy.calculateTax(new BigDecimal(salary));
            assertThat(tax).isEqualByComparingTo(new BigDecimal(expectedTax));
        }

        @Test
        @DisplayName("50 000.00 (exact low threshold) is MID, not LOW")
        void exactLowThreshold_isMid() {
            // 50 000 × 0.20 = 10 000  (not 50 000 × 0.10 = 5 000)
            assertThat(strategy.calculateTax(new BigDecimal("50000.00")))
                    .isEqualByComparingTo("10000.00");
        }

        @Test
        @DisplayName("100 000.00 (exact high threshold) is MID, not HIGH")
        void exactHighThreshold_isMid() {
            // 100 000 × 0.20 = 20 000  (not 100 000 × 0.30 = 30 000)
            assertThat(strategy.calculateTax(new BigDecimal("100000.00")))
                    .isEqualByComparingTo("20000.00");
        }
    }

    @Nested
    @DisplayName("High band (salary > 100 000) → 30%")
    class HighBand {

        @ParameterizedTest(name = "salary={0} → tax={1}")
        @CsvSource({
            "100000.01,   30000.00",   // just above threshold (rounds)
            "120000.00,   36000.00",   // 30 % of 120 000
            "200000.00,   60000.00",   // 30 % of 200 000
            "1000000.00, 300000.00",   // 30 % of 1 000 000
        })
        @DisplayName("parameterised high-band salaries")
        void highBand_thirtyPercent(String salary, String expectedTax) {
            BigDecimal tax = strategy.calculateTax(new BigDecimal(salary));
            assertThat(tax).isEqualByComparingTo(new BigDecimal(expectedTax));
        }

        @Test
        @DisplayName("100 000.01 is HIGH band, not MID")
        void justAboveHighThreshold_isHigh() {
            // 100 000.01 × 0.30 = 30 000.003  →  rounds to 30 000.00
            assertThat(strategy.calculateTax(new BigDecimal("100000.01")))
                    .isEqualByComparingTo("30000.00");
        }
    }

    // =========================================================================
    // 2. Boundary exhaustion (one value below, at, and above each threshold)
    // =========================================================================

    @Nested
    @DisplayName("Band boundary transitions")
    class BoundaryTransitions {

        @Test
        @DisplayName("49 999.99 → LOW  (10 %)")
        void justBelowLow() {
            assertThat(strategy.calculateTax(new BigDecimal("49999.99")))
                    .isEqualByComparingTo("5000.00");
        }

        @Test
        @DisplayName("50 000.00 → MID  (20 %)")
        void atLowThreshold() {
            assertThat(strategy.calculateTax(new BigDecimal("50000.00")))
                    .isEqualByComparingTo("10000.00");
        }

        @Test
        @DisplayName("50 000.01 → MID  (20 %)")
        void justAboveLow() {
            assertThat(strategy.calculateTax(new BigDecimal("50000.01")))
                    .isEqualByComparingTo("10000.00");
        }

        @Test
        @DisplayName("100 000.00 → MID  (20 %)")
        void atHighThreshold() {
            assertThat(strategy.calculateTax(new BigDecimal("100000.00")))
                    .isEqualByComparingTo("20000.00");
        }

        @Test
        @DisplayName("100 000.01 → HIGH (30 %)")
        void justAboveHigh() {
            assertThat(strategy.calculateTax(new BigDecimal("100000.01")))
                    .isEqualByComparingTo("30000.00");
        }
    }

    // =========================================================================
    // 3. TaxStrategyFactory integration
    // =========================================================================

    @Nested
    @DisplayName("TaxStrategyFactory — all roles use SalaryBandTaxStrategy")
    class FactoryIntegration {

        @Test
        @DisplayName("every Role resolves to a SalaryBandTaxStrategy instance")
        void allRoles_returnSalaryBandStrategy() {
            for (Role role : Role.values()) {
                TaxStrategy resolved = TaxStrategyFactory.forRole(role);
                assertThat(resolved)
                        .as("Role %s should use SalaryBandTaxStrategy", role)
                        .isInstanceOf(SalaryBandTaxStrategy.class);
            }
        }

        @ParameterizedTest(name = "Role {0}, salary {1} → tax {2}")
        @CsvSource({
            "ENGINEER,        40000, 4000.00",    // LOW  10 %
            "SENIOR_ENGINEER, 75000, 15000.00",   // MID  20 %
            "MANAGER,         75000, 15000.00",   // MID  20 %
            "SENIOR_MANAGER, 120000, 36000.00",   // HIGH 30 %
            "ANALYST,         30000, 3000.00",    // LOW  10 %
            "HR,              60000, 12000.00",   // MID  20 %
            "DIRECTOR,       150000, 45000.00",   // HIGH 30 %
        })
        @DisplayName("role + salary → expected tax (via factory)")
        void roleAndSalary_correctTax(Role role, String salary, String expectedTax) {
            TaxStrategy s = TaxStrategyFactory.forRole(role);
            assertThat(s.calculateTax(new BigDecimal(salary)))
                    .isEqualByComparingTo(new BigDecimal(expectedTax));
        }
    }

    // =========================================================================
    // 4. Full PayrollServiceImpl integration
    // =========================================================================

    @Nested
    @DisplayName("PayrollServiceImpl end-to-end tax calculation")
    class PayrollServiceIntegration {

        private final PayrollServiceImpl payrollService = new PayrollServiceImpl();
        private final LocalDate payrollMonth = LocalDate.of(2026, 2, 1);

        private Employee employee(String id, Role role, String salary) {
            return Employee.builder()
                    .id(id).name("Test User").email("test@example.com")
                    .departmentId("DEPT-01").role(role)
                    .salary(new BigDecimal(salary))
                    .status(EmployeeStatus.ACTIVE)
                    .joiningDate(LocalDate.of(2023, 1, 1))
                    .build();
        }

        @Test
        @DisplayName("ENGINEER salary=40 000 → gross=40 000, tax=4 000, net=36 000")
        void engineerLowBand() {
            PayrollRecord record = payrollService.process(
                    employee("E1", Role.ENGINEER, "40000"), payrollMonth);

            assertThat(record.getGrossSalary()).isEqualByComparingTo("40000");
            assertThat(record.getTaxAmount())  .isEqualByComparingTo("4000.00");
            assertThat(record.getNetSalary())  .isEqualByComparingTo("36000.00");
        }

        @Test
        @DisplayName("MANAGER salary=75 000 → gross=75 000, tax=15 000, net=60 000")
        void managerMidBand() {
            PayrollRecord record = payrollService.process(
                    employee("E2", Role.MANAGER, "75000"), payrollMonth);

            assertThat(record.getGrossSalary()).isEqualByComparingTo("75000");
            assertThat(record.getTaxAmount())  .isEqualByComparingTo("15000.00");
            assertThat(record.getNetSalary())  .isEqualByComparingTo("60000.00");
        }

        @Test
        @DisplayName("DIRECTOR salary=150 000 → gross=150 000, tax=45 000, net=105 000")
        void directorHighBand() {
            PayrollRecord record = payrollService.process(
                    employee("E3", Role.DIRECTOR, "150000"), payrollMonth);

            assertThat(record.getGrossSalary()).isEqualByComparingTo("150000");
            assertThat(record.getTaxAmount())  .isEqualByComparingTo("45000.00");
            assertThat(record.getNetSalary())  .isEqualByComparingTo("105000.00");
        }

        @Test
        @DisplayName("salary=50 000 (MID boundary) → tax=10 000")
        void exactLowBoundary_midBand() {
            PayrollRecord record = payrollService.process(
                    employee("E4", Role.SENIOR_ENGINEER, "50000"), payrollMonth);

            assertThat(record.getTaxAmount()).isEqualByComparingTo("10000.00");
        }

        @Test
        @DisplayName("salary=100 000 (HIGH boundary, inclusive MID) → tax=20 000")
        void exactHighBoundary_midBand() {
            PayrollRecord record = payrollService.process(
                    employee("E5", Role.SENIOR_MANAGER, "100000"), payrollMonth);

            assertThat(record.getTaxAmount()).isEqualByComparingTo("20000.00");
        }

        @Test
        @DisplayName("payroll record has non-null id and processedTimestamp")
        void recordMetadata_populated() {
            PayrollRecord record = payrollService.process(
                    employee("E6", Role.ANALYST, "30000"), payrollMonth);

            assertThat(record.getId()).isNotBlank();
            assertThat(record.getEmployeeId()).isEqualTo("E6");
            assertThat(record.getPayrollMonth()).isEqualTo(payrollMonth);
            assertThat(record.getProcessedTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("netSalary invariant: net == gross - tax always holds")
        void netSalaryInvariant() {
            for (String salary : new String[]{"30000", "75000", "120000"}) {
                PayrollRecord r = payrollService.process(
                        employee("EX", Role.ENGINEER, salary), payrollMonth);
                assertThat(r.getNetSalary())
                        .as("net must equal gross - tax for salary=%s", salary)
                        .isEqualByComparingTo(r.getGrossSalary().subtract(r.getTaxAmount()));
            }
        }
    }

    // =========================================================================
    // 5. Custom band configuration
    // =========================================================================

    @Nested
    @DisplayName("Custom band configuration")
    class CustomBands {

        @Test
        @DisplayName("custom thresholds and rates applied correctly")
        void customBands_correctTax() {
            // Custom: < 30 000 → 5 %,  30 000–80 000 → 15 %,  > 80 000 → 25 %
            var custom = new SalaryBandTaxStrategy(
                    new BigDecimal("30000"), new BigDecimal("80000"),
                    new BigDecimal("0.05"), new BigDecimal("0.15"), new BigDecimal("0.25"));

            assertThat(custom.calculateTax(new BigDecimal("20000")))
                    .isEqualByComparingTo("1000.00");   // 5 %

            assertThat(custom.calculateTax(new BigDecimal("50000")))
                    .isEqualByComparingTo("7500.00");   // 15 %

            assertThat(custom.calculateTax(new BigDecimal("100000")))
                    .isEqualByComparingTo("25000.00");  // 25 %
        }

        @Test
        @DisplayName("name() reflects configured thresholds")
        void name_reflectsConfiguration() {
            assertThat(strategy.name())
                    .contains("50000")
                    .contains("100000")
                    .contains("10%")
                    .contains("20%")
                    .contains("30%");
        }
    }

    // =========================================================================
    // 6. Input validation
    // =========================================================================

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        @Test
        @DisplayName("null grossSalary → NullPointerException")
        void nullSalary_throws() {
            assertThatThrownBy(() -> strategy.calculateTax(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("negative grossSalary → IllegalArgumentException")
        void negativeSalary_throws() {
            assertThatThrownBy(() -> strategy.calculateTax(new BigDecimal("-1")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("highThreshold <= lowThreshold → IllegalArgumentException")
        void invalidThresholds_throws() {
            assertThatThrownBy(() -> new SalaryBandTaxStrategy(
                    new BigDecimal("100000"), new BigDecimal("50000"),
                    new BigDecimal("0.10"), new BigDecimal("0.20"), new BigDecimal("0.30")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("highThreshold must be > lowThreshold");
        }

        @Test
        @DisplayName("rate > 1 → IllegalArgumentException")
        void rateAboveOne_throws() {
            assertThatThrownBy(() -> new SalaryBandTaxStrategy(
                    new BigDecimal("50000"), new BigDecimal("100000"),
                    new BigDecimal("1.50"), new BigDecimal("0.20"), new BigDecimal("0.30")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lowRate");
        }

        @Test
        @DisplayName("zero salary → zero tax (not an error)")
        void zeroSalary_zeroTax() {
            assertThat(strategy.calculateTax(BigDecimal.ZERO))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}

