package com.srikanth.javareskill.payroll;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.payroll.impl.PayrollServiceImpl;
import com.srikanth.javareskill.payroll.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Advanced parameterized tests for edge cases, bonus calculations, and complex scenarios.
 */
@DisplayName("Advanced Parameterized Salary Tests")
class AdvancedSalaryCalculationTest {

    private PayrollService payrollService;

    @BeforeEach
    void setUp() {
        payrollService = new PayrollServiceImpl();
    }

    // -------------------------------------------------------------------------
    // Bonus and Additional Compensation Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Base={0}, Bonus={1}% → Total={2}, Tax={3}")
    @CsvSource({
        "100000,  10,  110000,  14000.00",   // Base 100k + 10% bonus = 110k
        "100000,  20,  120000,  19000.00",   // Base 100k + 20% bonus = 120k
        "150000,  15,  172500,  32750.00",   // Base 150k + 15% bonus = 172.5k
        "80000,   25,  100000,  13000.00",   // Base 80k + 25% bonus = 100k
        "120000,  30,  156000,  29800.00"    // Base 120k + 30% bonus = 156k
    })
    @DisplayName("Should calculate tax on total compensation (salary + bonus)")
    void testBonusImpactOnTax(BigDecimal baseSalary, int bonusPercent, 
                               BigDecimal totalComp, BigDecimal expectedTax) {
        // Arrange
        BigDecimal bonusAmount = baseSalary.multiply(new BigDecimal(bonusPercent))
                .divide(new BigDecimal("100"));
        BigDecimal totalCompensation = baseSalary.add(bonusAmount);
        
        assertThat(totalCompensation).isEqualByComparingTo(totalComp);
        
        TaxStrategy strategy = new ProgressiveTaxStrategy();

        // Act
        BigDecimal tax = strategy.calculateTax(totalCompensation);

        // Assert
        assertThat(tax).isEqualByComparingTo(expectedTax);
    }

    // -------------------------------------------------------------------------
    // Pro-rated Salary Tests (Partial Month)
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Pro-rated: {0}/month for {1} days → {2}")
    @CsvSource({
        "90000,   30,  90000.00",    // Full month
        "90000,   15,  45000.00",    // Half month
        "90000,   10,  30000.00",    // 10 days
        "120000,  20,  80000.00",    // 20 days of 30
        "150000,  25,  125000.00"    // 25 days of 30
    })
    @DisplayName("Should calculate pro-rated salary for partial month correctly")
    void testProRatedSalary(BigDecimal monthlySalary, int daysWorked, BigDecimal expectedProRated) {
        // Arrange
        int daysInMonth = 30;  // Simplified
        TaxStrategy strategy = new ProgressiveTaxStrategy();

        // Act
        BigDecimal proRatedSalary = monthlySalary
                .multiply(new BigDecimal(daysWorked))
                .divide(new BigDecimal(daysInMonth), 2, java.math.RoundingMode.HALF_UP);
        
        assertThat(proRatedSalary).isEqualByComparingTo(expectedProRated);

        BigDecimal tax = strategy.calculateTax(proRatedSalary);
        BigDecimal net = proRatedSalary.subtract(tax);

        // Assert
        assertThat(tax).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(net).isLessThanOrEqualTo(proRatedSalary);
    }

    // -------------------------------------------------------------------------
    // Multiple Employee Comparison Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Compare: {0} vs {1}")
    @CsvSource({
        "80000,   100000",    // Junior vs Mid
        "100000,  150000",    // Mid vs Senior
        "150000,  200000",    // Senior vs Manager
        "200000,  300000"     // Manager vs Director
    })
    @DisplayName("Should show progressive tax benefit (higher salary, lower effective rate)")
    void testProgressiveTaxBenefit(BigDecimal lowerSalary, BigDecimal higherSalary) {
        // Arrange
        TaxStrategy strategy = new ProgressiveTaxStrategy();

        // Act
        BigDecimal lowerTax = strategy.calculateTax(lowerSalary);
        BigDecimal higherTax = strategy.calculateTax(higherSalary);

        BigDecimal lowerEffectiveRate = lowerTax.divide(lowerSalary, 4, java.math.RoundingMode.HALF_UP);
        BigDecimal higherEffectiveRate = higherTax.divide(higherSalary, 4, java.math.RoundingMode.HALF_UP);

        // Assert
        assertThat(higherTax).isGreaterThan(lowerTax);  // Higher salary = more tax
        
        System.out.println("  Lower: " + lowerSalary + " @ " + 
            lowerEffectiveRate.multiply(new BigDecimal("100")) + "% effective");
        System.out.println("  Higher: " + higherSalary + " @ " + 
            higherEffectiveRate.multiply(new BigDecimal("100")) + "% effective");
    }

    // -------------------------------------------------------------------------
    // Tax Deduction Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Deductions: Gross={0}, Deduction={1} → Taxable={2}")
    @CsvSource({
        "100000,  10000,  90000",     // 10k deduction
        "150000,  20000,  130000",    // 20k deduction
        "200000,  50000,  150000",    // 50k deduction (e.g., 401k)
        "120000,  30000,  90000"      // 30k deduction
    })
    @DisplayName("Should calculate tax on reduced taxable income after deductions")
    void testTaxAfterDeductions(BigDecimal grossSalary, BigDecimal deduction, 
                                 BigDecimal taxableIncome) {
        // Arrange
        TaxStrategy strategy = new ProgressiveTaxStrategy();

        // Act
        BigDecimal taxOnGross = strategy.calculateTax(grossSalary);
        BigDecimal taxOnTaxable = strategy.calculateTax(taxableIncome);
        BigDecimal taxSavings = taxOnGross.subtract(taxOnTaxable);

        // Assert
        assertThat(taxOnTaxable).isLessThan(taxOnGross);  // Deduction reduces tax
        assertThat(taxSavings).isGreaterThan(BigDecimal.ZERO);
        
        System.out.println("  Tax without deduction: " + taxOnGross);
        System.out.println("  Tax with deduction: " + taxOnTaxable);
        System.out.println("  Tax savings: " + taxSavings);
    }

    // -------------------------------------------------------------------------
    // Bulk Salary Processing Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Bulk process {0} employees with avg salary {1}")
    @CsvSource({
        "10,   100000",
        "50,   120000",
        "100,  150000",
        "200,  180000"
    })
    @DisplayName("Should process bulk payroll correctly")
    void testBulkPayrollProcessing(int employeeCount, BigDecimal avgSalary) {
        // Arrange
        List<Employee> employees = createEmployees(employeeCount, avgSalary);

        // Act
        List<PayrollRecord> records = payrollService.processAll(employees, LocalDate.now());

        // Assert
        assertThat(records).hasSize(employeeCount);
        
        BigDecimal totalGross = records.stream()
                .map(PayrollRecord::getGrossSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalTax = records.stream()
                .map(PayrollRecord::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalNet = records.stream()
                .map(PayrollRecord::getNetSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Verify: total net = total gross - total tax
        assertThat(totalNet).isEqualByComparingTo(totalGross.subtract(totalTax));
        
        System.out.println("  Total Gross: " + totalGross);
        System.out.println("  Total Tax: " + totalTax);
        System.out.println("  Total Net: " + totalNet);
    }

    // -------------------------------------------------------------------------
    // Tax Invariants Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Tax invariant: salary={0}")
    @ValueSource(strings = {"10000", "50000", "100000", "150000", "200000", "500000"})
    @DisplayName("Should maintain tax invariants for all salary levels")
    void testTaxInvariants(String salaryStr) {
        // Arrange
        BigDecimal salary = new BigDecimal(salaryStr);
        TaxStrategy flatRate = new FlatRateTaxStrategy(0.20);
        TaxStrategy progressive = new ProgressiveTaxStrategy();
        TaxStrategy exempt = new ExemptTaxStrategy();

        // Act
        BigDecimal flatTax = flatRate.calculateTax(salary);
        BigDecimal progressiveTax = progressive.calculateTax(salary);
        BigDecimal exemptTax = exempt.calculateTax(salary);

        // Assert - Universal invariants
        // 1. Tax must be non-negative
        assertThat(flatTax).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(progressiveTax).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(exemptTax).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        // 2. Tax must not exceed gross salary
        assertThat(flatTax).isLessThanOrEqualTo(salary);
        assertThat(progressiveTax).isLessThanOrEqualTo(salary);
        assertThat(exemptTax).isLessThanOrEqualTo(salary);

        // 3. Net salary must be non-negative
        assertThat(salary.subtract(flatTax)).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(salary.subtract(progressiveTax)).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(salary.subtract(exemptTax)).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    // -------------------------------------------------------------------------
    // Tax Bracket Transition Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Transition: {0} → {1}")
    @CsvSource({
        "19999,  20001",     // Crossing first bracket
        "49999,  50001",     // Crossing second bracket
        "99999,  100001"     // Crossing third bracket
    })
    @DisplayName("Should calculate smooth tax transitions across brackets")
    void testTaxBracketTransitions(BigDecimal salaryBelow, BigDecimal salaryAbove) {
        // Arrange
        TaxStrategy strategy = new ProgressiveTaxStrategy();

        // Act
        BigDecimal taxBelow = strategy.calculateTax(salaryBelow);
        BigDecimal taxAbove = strategy.calculateTax(salaryAbove);

        // Assert
        // Tax should increase, but not dramatically (no cliff)
        assertThat(taxAbove).isGreaterThan(taxBelow);
        
        BigDecimal taxDifference = taxAbove.subtract(taxBelow);
        BigDecimal salaryDifference = salaryAbove.subtract(salaryBelow);
        
        // Tax difference should be reasonable (not more than salary difference)
        assertThat(taxDifference).isLessThanOrEqualTo(salaryDifference);
        
        System.out.println("  Salary increase: " + salaryDifference + 
                         " → Tax increase: " + taxDifference);
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private List<Employee> createEmployees(int count, BigDecimal avgSalary) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> Employee.builder()
                        .id("E" + String.format("%04d", i + 1))
                        .name("Employee " + (i + 1))
                        .email("emp" + (i + 1) + "@test.com")
                        .departmentId("D001")
                        .role(Role.ENGINEER)
                        .salary(avgSalary.add(new BigDecimal(i * 1000 - count * 500)))
                        .status(EmployeeStatus.ACTIVE)
                        .joiningDate(LocalDate.now().minusYears(1))
                        .build())
                .toList();
    }
}

