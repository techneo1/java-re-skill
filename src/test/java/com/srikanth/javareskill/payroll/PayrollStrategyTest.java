package com.srikanth.javareskill.payroll;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.payroll.impl.PayrollServiceImpl;
import com.srikanth.javareskill.payroll.strategy.ExemptTaxStrategy;
import com.srikanth.javareskill.payroll.strategy.FlatRateTaxStrategy;
import com.srikanth.javareskill.payroll.strategy.ProgressiveTaxStrategy;
import com.srikanth.javareskill.payroll.strategy.TaxStrategy;
import com.srikanth.javareskill.payroll.strategy.TaxStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PayrollStrategy – Strategy Pattern Tests")
class PayrollStrategyTest {

    // =========================================================================
    // Helpers
    // =========================================================================

    private static final LocalDate APRIL_2026 = LocalDate.of(2026, 4, 1);

    private static Employee employee(Role role, String salary) {
        return Employee.builder()
                .id("EMP-001")
                .name("Test Employee")
                .email("test@example.com")
                .departmentId("DEPT-001")
                .role(role)
                .salary(new BigDecimal(salary))
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2020, 1, 1))
                .build();
    }

    // =========================================================================
    // FlatRateTaxStrategy
    // =========================================================================

    @Nested
    @DisplayName("FlatRateTaxStrategy")
    class FlatRateTaxStrategyTest {

        @Test
        @DisplayName("calculates 20% tax correctly")
        void twentyPercentFlat() {
            TaxStrategy strategy = new FlatRateTaxStrategy(0.20);
            BigDecimal tax = strategy.calculateTax(new BigDecimal("50000"));
            assertThat(tax).isEqualByComparingTo("10000.00");
        }

        @Test
        @DisplayName("calculates 10% tax correctly")
        void tenPercentFlat() {
            TaxStrategy strategy = new FlatRateTaxStrategy(0.10);
            BigDecimal tax = strategy.calculateTax(new BigDecimal("30000"));
            assertThat(tax).isEqualByComparingTo("3000.00");
        }

        @Test
        @DisplayName("zero rate returns zero tax")
        void zeroRate() {
            TaxStrategy strategy = new FlatRateTaxStrategy(0.0);
            assertThat(strategy.calculateTax(new BigDecimal("100000"))).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("100% rate returns full gross salary as tax")
        void fullRate() {
            TaxStrategy strategy = new FlatRateTaxStrategy(1.0);
            assertThat(strategy.calculateTax(new BigDecimal("1000"))).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("name returns human-readable label")
        void nameLabel() {
            assertThat(new FlatRateTaxStrategy(0.20).name()).isEqualTo("FlatRate(20%)");
        }

        @Test
        @DisplayName("rejects negative rate")
        void negativeRateRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new FlatRateTaxStrategy(-0.01));
        }

        @Test
        @DisplayName("rejects rate above 1")
        void rateAboveOneRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new FlatRateTaxStrategy(1.01));
        }

        @Test
        @DisplayName("result is rounded to 2 decimal places (HALF_UP)")
        void roundingApplied() {
            // 0.15 * 33333 = 4999.95
            TaxStrategy strategy = new FlatRateTaxStrategy(0.15);
            BigDecimal tax = strategy.calculateTax(new BigDecimal("33333"));
            assertThat(tax.scale()).isEqualTo(2);
        }
    }

    // =========================================================================
    // ProgressiveTaxStrategy
    // =========================================================================

    @Nested
    @DisplayName("ProgressiveTaxStrategy")
    class ProgressiveTaxStrategyTests {

        private TaxStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new ProgressiveTaxStrategy(); // default brackets
        }

        @Test
        @DisplayName("salary within first bracket (0%) incurs no tax")
        void firstBracketZeroTax() {
            BigDecimal tax = strategy.calculateTax(new BigDecimal("15000"));
            assertThat(tax).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("salary exactly at first bracket upper bound")
        void exactFirstBracketBoundary() {
            BigDecimal tax = strategy.calculateTax(new BigDecimal("20000"));
            assertThat(tax).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("salary spanning first two brackets is taxed correctly")
        void twoBrackets() {
            // 0–20000 @ 0% = 0, 20000–30000 (10000) @ 10% = 1000  → total = 1000
            BigDecimal tax = strategy.calculateTax(new BigDecimal("30000"));
            assertThat(tax).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("salary spanning three brackets is taxed correctly")
        void threeBrackets() {
            // 0–20000 @ 0% = 0
            // 20000–50000 (30000) @ 10% = 3000
            // 50000–75000 (25000) @ 20% = 5000
            // total = 8000
            BigDecimal tax = strategy.calculateTax(new BigDecimal("75000"));
            assertThat(tax).isEqualByComparingTo("8000.00");
        }

        @Test
        @DisplayName("salary spanning all four brackets (top bracket) is taxed correctly")
        void allBrackets() {
            // 0–20000 @ 0%   = 0
            // 20000–50000 (30000) @ 10% = 3000
            // 50000–100000 (50000) @ 20% = 10000
            // 100000–150000 (50000) @ 30% = 15000
            // total = 28000
            BigDecimal tax = strategy.calculateTax(new BigDecimal("150000"));
            assertThat(tax).isEqualByComparingTo("28000.00");
        }

        @Test
        @DisplayName("name describes bracket count")
        void nameDescribesBrackets() {
            assertThat(strategy.name()).isEqualTo("Progressive(4 brackets)");
        }

        @Test
        @DisplayName("custom brackets work correctly")
        void customBrackets() {
            List<ProgressiveTaxStrategy.Bracket> custom = List.of(
                    new ProgressiveTaxStrategy.Bracket(BigDecimal.ZERO,        new BigDecimal("10000"), new BigDecimal("0.05")),
                    new ProgressiveTaxStrategy.Bracket(new BigDecimal("10000"), null,                   new BigDecimal("0.25"))
            );
            TaxStrategy customStrategy = new ProgressiveTaxStrategy(custom);
            // 0-10000 @ 5% = 500, 10000-20000 (10000) @ 25% = 2500 → total = 3000
            assertThat(customStrategy.calculateTax(new BigDecimal("20000")))
                    .isEqualByComparingTo("3000.00");
        }

        @Test
        @DisplayName("empty brackets list is rejected")
        void emptyBracketsRejected() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new ProgressiveTaxStrategy(List.of()));
        }
    }

    // =========================================================================
    // ExemptTaxStrategy
    // =========================================================================

    @Nested
    @DisplayName("ExemptTaxStrategy")
    class ExemptTaxStrategyTests {

        @Test
        @DisplayName("always returns zero regardless of gross salary")
        void alwaysZero() {
            TaxStrategy strategy = ExemptTaxStrategy.INSTANCE;
            assertThat(strategy.calculateTax(new BigDecimal("999999"))).isEqualByComparingTo("0.00");
            assertThat(strategy.calculateTax(BigDecimal.ZERO)).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("name is 'Exempt(0%)'")
        void nameLabel() {
            assertThat(ExemptTaxStrategy.INSTANCE.name()).isEqualTo("Exempt(0%)");
        }
    }

    // =========================================================================
    // TaxStrategyFactory
    // =========================================================================

    @Nested
    @DisplayName("TaxStrategyFactory")
    class TaxStrategyFactoryTest {

        @Test
        @DisplayName("DIRECTOR resolves to ProgressiveTaxStrategy")
        void directorIsProgressive() {
            assertThat(TaxStrategyFactory.forRole(Role.DIRECTOR))
                    .isInstanceOf(ProgressiveTaxStrategy.class);
        }

        @Test
        @DisplayName("SENIOR_MANAGER resolves to ProgressiveTaxStrategy")
        void seniorManagerIsProgressive() {
            assertThat(TaxStrategyFactory.forRole(Role.SENIOR_MANAGER))
                    .isInstanceOf(ProgressiveTaxStrategy.class);
        }

        @Test
        @DisplayName("MANAGER resolves to FlatRateTaxStrategy(20%)")
        void managerIsFlatTwenty() {
            TaxStrategy strategy = TaxStrategyFactory.forRole(Role.MANAGER);
            assertThat(strategy).isInstanceOf(FlatRateTaxStrategy.class);
            assertThat(((FlatRateTaxStrategy) strategy).getRate())
                    .isEqualByComparingTo("0.20");
        }

        @Test
        @DisplayName("ENGINEER resolves to FlatRateTaxStrategy(10%)")
        void engineerIsFlatTen() {
            TaxStrategy strategy = TaxStrategyFactory.forRole(Role.ENGINEER);
            assertThat(strategy).isInstanceOf(FlatRateTaxStrategy.class);
            assertThat(((FlatRateTaxStrategy) strategy).getRate())
                    .isEqualByComparingTo("0.10");
        }

        @Test
        @DisplayName("all roles are covered")
        void allRolesCovered() {
            for (Role role : Role.values()) {
                assertThatNoException()
                        .as("Role %s should have a strategy", role)
                        .isThrownBy(() -> TaxStrategyFactory.forRole(role));
            }
        }
    }

    // =========================================================================
    // PayrollServiceImpl – integration
    // =========================================================================

    @Nested
    @DisplayName("PayrollServiceImpl")
    class PayrollServiceImplTest {

        private PayrollService payrollService;

        @BeforeEach
        void setUp() {
            payrollService = new PayrollServiceImpl();
        }

        @Test
        @DisplayName("process() with explicit strategy produces correct PayrollRecord")
        void processWithExplicitStrategy() {
            Employee emp = employee(Role.ENGINEER, "40000");
            TaxStrategy flat20 = new FlatRateTaxStrategy(0.20);

            PayrollRecord record = payrollService.process(emp, APRIL_2026, flat20);

            assertThat(record.getEmployeeId()).isEqualTo("EMP-001");
            assertThat(record.getGrossSalary()).isEqualByComparingTo("40000");
            assertThat(record.getTaxAmount()).isEqualByComparingTo("8000.00");
            assertThat(record.getNetSalary()).isEqualByComparingTo("32000.00");
            assertThat(record.getPayrollMonth()).isEqualTo(APRIL_2026);
        }

        @Test
        @DisplayName("process() with default strategy uses role-based strategy")
        void processWithDefaultStrategy() {
            Employee manager = employee(Role.MANAGER, "60000");
            // MANAGER → Flat 20% → tax = 12000
            PayrollRecord record = payrollService.process(manager, APRIL_2026);

            assertThat(record.getTaxAmount()).isEqualByComparingTo("12000.00");
            assertThat(record.getNetSalary()).isEqualByComparingTo("48000.00");
        }

        @Test
        @DisplayName("process() with ExemptTaxStrategy yields zero tax")
        void processExempt() {
            Employee emp = employee(Role.ANALYST, "35000");
            PayrollRecord record = payrollService.process(emp, APRIL_2026, ExemptTaxStrategy.INSTANCE);

            assertThat(record.getTaxAmount()).isEqualByComparingTo("0.00");
            assertThat(record.getNetSalary()).isEqualByComparingTo("35000");
        }

        @Test
        @DisplayName("netSalary invariant: netSalary == grossSalary - taxAmount")
        void netSalaryInvariant() {
            Employee emp = employee(Role.DIRECTOR, "200000");
            TaxStrategy progressive = new ProgressiveTaxStrategy();

            PayrollRecord record = payrollService.process(emp, APRIL_2026, progressive);

            BigDecimal expected = record.getGrossSalary().subtract(record.getTaxAmount());
            assertThat(record.getNetSalary()).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("processAll() returns one record per employee")
        void processAll() {
            List<Employee> employees = List.of(
                    employee(Role.ENGINEER, "30000"),
                    employee(Role.MANAGER, "70000"),
                    employee(Role.DIRECTOR, "180000")
            );
            List<PayrollRecord> records = payrollService.processAll(employees, APRIL_2026);

            assertThat(records).hasSize(3);
        }

        @Test
        @DisplayName("PayrollRecord has a non-null ID and processedTimestamp")
        void recordMetadata() {
            Employee emp = employee(Role.HR, "25000");
            PayrollRecord record = payrollService.process(emp, APRIL_2026);

            assertThat(record.getId()).isNotBlank();
            assertThat(record.getProcessedTimestamp()).isNotNull()
                    .isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("payrollMonth is normalised to first day of month")
        void payrollMonthNormalised() {
            Employee emp = employee(Role.ENGINEER, "30000");
            LocalDate midMonth = LocalDate.of(2026, 4, 15);
            PayrollRecord record = payrollService.process(emp, midMonth, ExemptTaxStrategy.INSTANCE);

            assertThat(record.getPayrollMonth()).isEqualTo(LocalDate.of(2026, 4, 1));
        }

        @Test
        @DisplayName("strategy can be swapped at runtime (Strategy Pattern demo)")
        void strategyIsSwappable() {
            Employee emp = employee(Role.SENIOR_ENGINEER, "80000");

            TaxStrategy flat10 = new FlatRateTaxStrategy(0.10);
            TaxStrategy flat30 = new FlatRateTaxStrategy(0.30);

            BigDecimal tax10 = payrollService.process(emp, APRIL_2026, flat10).getTaxAmount();
            BigDecimal tax30 = payrollService.process(emp, APRIL_2026, flat30).getTaxAmount();

            assertThat(tax10).isEqualByComparingTo("8000.00");
            assertThat(tax30).isEqualByComparingTo("24000.00");
            assertThat(tax30).isGreaterThan(tax10);
        }
    }
}

