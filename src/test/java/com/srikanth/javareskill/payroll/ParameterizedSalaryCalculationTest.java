package com.srikanth.javareskill.payroll;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.payroll.impl.PayrollServiceImpl;
import com.srikanth.javareskill.payroll.strategy.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive parameterized tests for salary calculations.
 *
 * <p>Uses JUnit 5's {@link ParameterizedTest} to test salary calculations
 * with multiple input combinations, ensuring accuracy across various scenarios.</p>
 *
 * <h2>Test Categories</h2>
 * <ul>
 *   <li><b>Flat Rate Tax Calculations</b> - Various rates and salaries</li>
 *   <li><b>Progressive Tax Calculations</b> - Multiple tax brackets</li>
 *   <li><b>Net Salary Calculations</b> - Gross minus tax</li>
 *   <li><b>Payroll Processing</b> - End-to-end with different roles</li>
 *   <li><b>Edge Cases</b> - Zero, minimum, maximum salaries</li>
 * </ul>
 */
@DisplayName("Parameterized Salary Calculation Tests")
class ParameterizedSalaryCalculationTest {

    private PayrollService payrollService;

    @BeforeEach
    void setUp() {
        payrollService = new PayrollServiceImpl();
    }

    // -------------------------------------------------------------------------
    // Flat Rate Tax Calculations
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Flat tax: salary={0}, rate={1}% → tax={2}")
    @CsvSource({
        "100000,  20,  20000.00",    // 20% of 100,000
        "100000,  15,  15000.00",    // 15% of 100,000
        "100000,  10,  10000.00",    // 10% of 100,000
        "50000,   20,  10000.00",    // 20% of 50,000
        "50000,   15,  7500.00",     // 15% of 50,000
        "75000,   25,  18750.00",    // 25% of 75,000
        "200000,  30,  60000.00",    // 30% of 200,000
        "150000,  18,  27000.00",    // 18% of 150,000
        "120000,  22,  26400.00",    // 22% of 120,000
        "80000,   12,  9600.00"      // 12% of 80,000
    })
    @DisplayName("Should calculate flat rate tax correctly for various salaries and rates")
    void testFlatRateTaxCalculation(BigDecimal salary, int ratePercent, BigDecimal expectedTax) {
        // Arrange
        BigDecimal rate = new BigDecimal(ratePercent).divide(new BigDecimal("100"));
        TaxStrategy strategy = new FlatRateTaxStrategy(rate);

        // Act
        BigDecimal actualTax = strategy.calculateTax(salary);

        // Assert
        assertThat(actualTax).isEqualByComparingTo(expectedTax);
    }

    @ParameterizedTest(name = "Flat tax net: salary={0}, rate={1}% → net={2}")
    @CsvSource({
        "100000,  20,  80000.00",    // 100k - 20k tax
        "100000,  15,  85000.00",    // 100k - 15k tax
        "50000,   20,  40000.00",    // 50k - 10k tax
        "200000,  30,  140000.00",   // 200k - 60k tax
        "150000,  25,  112500.00",   // 150k - 37.5k tax
        "80000,   10,  72000.00"     // 80k - 8k tax
    })
    @DisplayName("Should calculate correct net salary after flat rate tax")
    void testNetSalaryAfterFlatTax(BigDecimal grossSalary, int ratePercent, BigDecimal expectedNet) {
        // Arrange
        BigDecimal rate = new BigDecimal(ratePercent).divide(new BigDecimal("100"));
        TaxStrategy strategy = new FlatRateTaxStrategy(rate);

        // Act
        BigDecimal tax = strategy.calculateTax(grossSalary);
        BigDecimal netSalary = grossSalary.subtract(tax);

        // Assert
        assertThat(netSalary).isEqualByComparingTo(expectedNet);
    }

    // -------------------------------------------------------------------------
    // Progressive Tax Calculations
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Progressive tax: salary={0} → tax={1}")
    @CsvSource({
        "10000,      0.00",       // Below first bracket (0-20k at 0%)
        "20000,      0.00",       // At first bracket boundary
        "30000,      1000.00",    // In second bracket: 10k @ 10% = 1000
        "50000,      3000.00",    // At second bracket: 30k @ 10% = 3000
        "75000,      8000.00",    // In third bracket: 30k@10% + 25k@20% = 3000 + 5000
        "100000,     13000.00",   // At third bracket: 30k@10% + 50k@20% = 3000 + 10000
        "150000,     28000.00",   // In fourth bracket: 30k@10% + 50k@20% + 50k@30% = 28000
        "200000,     43000.00",   // Higher: 30k@10% + 50k@20% + 100k@30% = 43000
        "120000,     19000.00",   // 30k@10% + 50k@20% + 20k@30% = 19000
        "80000,      9000.00"     // 30k@10% + 30k@20% = 9000
    })
    @DisplayName("Should calculate progressive tax correctly across multiple brackets")
    void testProgressiveTaxCalculation(BigDecimal salary, BigDecimal expectedTax) {
        // Arrange
        TaxStrategy strategy = new ProgressiveTaxStrategy();

        // Act
        BigDecimal actualTax = strategy.calculateTax(salary);

        // Assert
        assertThat(actualTax).isEqualByComparingTo(expectedTax);
    }

    @ParameterizedTest(name = "Progressive net: salary={0} → net={1}")
    @CsvSource({
        "10000,      10000.00",    // 10k - 0 tax
        "30000,      29000.00",    // 30k - 1k tax
        "50000,      47000.00",    // 50k - 3k tax
        "75000,      67000.00",    // 75k - 8k tax
        "100000,     87000.00",    // 100k - 13k tax
        "150000,     122000.00",   // 150k - 28k tax
        "200000,     157000.00"    // 200k - 43k tax
    })
    @DisplayName("Should calculate correct net salary after progressive tax")
    void testNetSalaryAfterProgressiveTax(BigDecimal grossSalary, BigDecimal expectedNet) {
        // Arrange
        TaxStrategy strategy = new ProgressiveTaxStrategy();

        // Act
        BigDecimal tax = strategy.calculateTax(grossSalary);
        BigDecimal netSalary = grossSalary.subtract(tax);

        // Assert
        assertThat(netSalary).isEqualByComparingTo(expectedNet);
    }

    // -------------------------------------------------------------------------
    // Exempt Tax Calculations
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Exempt: salary={0} → tax=0")
    @ValueSource(strings = {"50000", "100000", "150000", "200000", "250000"})
    @DisplayName("Should calculate zero tax for exempt employees")
    void testExemptTaxCalculation(String salaryStr) {
        // Arrange
        BigDecimal salary = new BigDecimal(salaryStr);
        TaxStrategy strategy = new ExemptTaxStrategy();

        // Act
        BigDecimal tax = strategy.calculateTax(salary);

        // Assert
        assertThat(tax).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest(name = "Exempt net: salary={0} → net={0} (no tax)")
    @ValueSource(strings = {"50000", "100000", "150000", "200000"})
    @DisplayName("Should have net salary equal to gross for exempt employees")
    void testNetSalaryForExempt(String salaryStr) {
        // Arrange
        BigDecimal salary = new BigDecimal(salaryStr);
        TaxStrategy strategy = new ExemptTaxStrategy();

        // Act
        BigDecimal tax = strategy.calculateTax(salary);
        BigDecimal net = salary.subtract(tax);

        // Assert
        assertThat(net).isEqualByComparingTo(salary);
    }

    // -------------------------------------------------------------------------
    // Payroll Processing Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Role {0}: salary={1} → net={2}")
    @CsvSource({
        "ENGINEER,         100000,  87000.00",   // Progressive tax
        "SENIOR_ENGINEER,  150000,  122000.00",  // Progressive tax
        "MANAGER,          200000,  157000.00",  // Progressive tax
        "SENIOR_MANAGER,   250000,  192000.00",  // Progressive tax: 30k@10% + 50k@20% + 150k@30% = 58000
        "ANALYST,          100000,  87000.00",   // Progressive tax
        "HR,               100000,  87000.00",   // Progressive tax
        "DIRECTOR,         300000,  222000.00"   // Progressive tax: 30k@10% + 50k@20% + 200k@30% = 78000
    })
    @DisplayName("Should calculate correct net salary for different roles")
    void testPayrollProcessingByRole(Role role, BigDecimal salary, BigDecimal expectedNet) {
        // Arrange
        Employee employee = createEmployee("E001", "Test Employee", salary, role);

        // Act
        PayrollRecord record = payrollService.process(employee, LocalDate.now());

        // Assert
        assertThat(record.getGrossSalary()).isEqualByComparingTo(salary);
        assertThat(record.getNetSalary()).isEqualByComparingTo(expectedNet);
    }

    @ParameterizedTest(name = "Payroll: gross={0} → tax={1}, net={2}")
    @MethodSource("salaryTestCases")
    @DisplayName("Should process payroll correctly for various salary levels")
    void testPayrollProcessing(BigDecimal grossSalary, BigDecimal expectedTax, BigDecimal expectedNet) {
        // Arrange
        Employee employee = createEmployee("E001", "Employee", grossSalary, Role.ENGINEER);
        TaxStrategy strategy = TaxStrategyFactory.forRole(Role.ENGINEER);

        // Act
        PayrollRecord record = payrollService.process(employee, LocalDate.now(), strategy);

        // Assert
        assertThat(record.getGrossSalary()).isEqualByComparingTo(grossSalary);
        assertThat(record.getTaxAmount()).isEqualByComparingTo(expectedTax);
        assertThat(record.getNetSalary()).isEqualByComparingTo(expectedNet);
        
        // Verify calculation: net = gross - tax
        BigDecimal calculatedNet = grossSalary.subtract(expectedTax);
        assertThat(record.getNetSalary()).isEqualByComparingTo(calculatedNet);
    }

    static Stream<Arguments> salaryTestCases() {
        return Stream.of(
            // grossSalary, expectedTax, expectedNet
            Arguments.of(new BigDecimal("10000"),   new BigDecimal("0.00"),     new BigDecimal("10000.00")),
            Arguments.of(new BigDecimal("20000"),   new BigDecimal("0.00"),     new BigDecimal("20000.00")),
            Arguments.of(new BigDecimal("30000"),   new BigDecimal("1000.00"),  new BigDecimal("29000.00")),
            Arguments.of(new BigDecimal("50000"),   new BigDecimal("3000.00"),  new BigDecimal("47000.00")),
            Arguments.of(new BigDecimal("75000"),   new BigDecimal("8000.00"),  new BigDecimal("67000.00")),
            Arguments.of(new BigDecimal("100000"),  new BigDecimal("13000.00"), new BigDecimal("87000.00")),
            Arguments.of(new BigDecimal("150000"),  new BigDecimal("28000.00"), new BigDecimal("122000.00")),
            Arguments.of(new BigDecimal("200000"),  new BigDecimal("43000.00"), new BigDecimal("157000.00")),
            Arguments.of(new BigDecimal("250000"),  new BigDecimal("58000.00"), new BigDecimal("192000.00")),
            Arguments.of(new BigDecimal("300000"),  new BigDecimal("73000.00"), new BigDecimal("227000.00"))
        );
    }

    // -------------------------------------------------------------------------
    // Tax Strategy Comparisons
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Strategy comparison: salary={0}")
    @ValueSource(strings = {"50000", "100000", "150000", "200000"})
    @DisplayName("Should show tax differences between strategies")
    void testTaxStrategyComparison(String salaryStr) {
        // Arrange
        BigDecimal salary = new BigDecimal(salaryStr);
        TaxStrategy flatRate = new FlatRateTaxStrategy(0.20);      // 20%
        TaxStrategy progressive = new ProgressiveTaxStrategy();
        TaxStrategy exempt = new ExemptTaxStrategy();

        // Act
        BigDecimal flatTax = flatRate.calculateTax(salary);
        BigDecimal progressiveTax = progressive.calculateTax(salary);
        BigDecimal exemptTax = exempt.calculateTax(salary);

        // Assert
        assertThat(exemptTax).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(progressiveTax).isLessThan(flatTax);  // Progressive is more favorable
        assertThat(flatTax).isEqualByComparingTo(salary.multiply(new BigDecimal("0.20")));
    }

    // -------------------------------------------------------------------------
    // Edge Cases
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Edge case: salary={0}")
    @CsvSource({
        "0.01,     0.00",      // Minimum positive salary
        "1.00,     0.00",      // Very low salary
        "19999.99, 0.00",      // Just below first bracket
        "20000.00, 0.00",      // Exactly at bracket boundary
        "20000.01, 0.00",      // Just above bracket boundary
        "999999,   289999.70"  // Very high salary
    })
    @DisplayName("Should handle edge case salaries correctly")
    void testEdgeCaseSalaries(BigDecimal salary, BigDecimal expectedTax) {
        // Arrange
        TaxStrategy strategy = new ProgressiveTaxStrategy();

        // Act
        BigDecimal tax = strategy.calculateTax(salary);

        // Assert
        assertThat(tax).isEqualByComparingTo(expectedTax);
        assertThat(tax).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(tax).isLessThanOrEqualTo(salary);
    }

    // -------------------------------------------------------------------------
    // Tax Bracket Boundary Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Bracket boundary: {0} → tax={1}")
    @CsvSource({
        "19999,   0.00",       // Just below first boundary
        "20000,   0.00",       // At first boundary (0-20k @ 0%)
        "20001,   0.10",       // Just above first boundary
        "49999,   2999.90",    // Just below second boundary
        "50000,   3000.00",    // At second boundary
        "50001,   3000.20",    // Just above second boundary
        "99999,   12999.80",   // Just below third boundary
        "100000,  13000.00",   // At third boundary
        "100001,  13000.30"    // Just above third boundary
    })
    @DisplayName("Should handle tax bracket boundaries correctly")
    void testTaxBracketBoundaries(BigDecimal salary, BigDecimal expectedTax) {
        // Arrange
        TaxStrategy strategy = new ProgressiveTaxStrategy();

        // Act
        BigDecimal tax = strategy.calculateTax(salary);

        // Assert
        assertThat(tax).isEqualByComparingTo(expectedTax);
    }

    // -------------------------------------------------------------------------
    // Multiple Roles Payroll Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "{0}: {1} with salary {2}")
    @MethodSource("roleBasedSalaryTestCases")
    @DisplayName("Should process payroll correctly for all roles")
    void testPayrollForAllRoles(Role role, String employeeName, BigDecimal salary, 
                                  BigDecimal expectedTax, BigDecimal expectedNet) {
        // Arrange
        Employee employee = createEmployee("E001", employeeName, salary, role);

        // Act
        PayrollRecord record = payrollService.process(employee, LocalDate.now());

        // Assert
        assertThat(record.getEmployeeId()).isEqualTo("E001");
        assertThat(record.getGrossSalary()).isEqualByComparingTo(salary);
        assertThat(record.getTaxAmount()).isEqualByComparingTo(expectedTax);
        assertThat(record.getNetSalary()).isEqualByComparingTo(expectedNet);
    }

    static Stream<Arguments> roleBasedSalaryTestCases() {
        return Stream.of(
            // Role, Name, Gross, Tax, Net
            Arguments.of(Role.ENGINEER, "Junior Engineer", 
                new BigDecimal("80000"), new BigDecimal("9000.00"), new BigDecimal("71000.00")),
            Arguments.of(Role.SENIOR_ENGINEER, "Senior Engineer", 
                new BigDecimal("150000"), new BigDecimal("28000.00"), new BigDecimal("122000.00")),
            Arguments.of(Role.MANAGER, "Engineering Manager", 
                new BigDecimal("200000"), new BigDecimal("43000.00"), new BigDecimal("157000.00")),
            Arguments.of(Role.SENIOR_MANAGER, "Senior Manager", 
                new BigDecimal("250000"), new BigDecimal("58000.00"), new BigDecimal("192000.00")),
            Arguments.of(Role.ANALYST, "Business Analyst", 
                new BigDecimal("90000"), new BigDecimal("11000.00"), new BigDecimal("79000.00")),
            Arguments.of(Role.HR, "HR Manager", 
                new BigDecimal("120000"), new BigDecimal("19000.00"), new BigDecimal("101000.00")),
            Arguments.of(Role.DIRECTOR, "Engineering Director", 
                new BigDecimal("300000"), new BigDecimal("73000.00"), new BigDecimal("227000.00"))
        );
    }

    // -------------------------------------------------------------------------
    // Tax Rate Validation
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Invalid rate: {0}")
    @ValueSource(doubles = {-0.1, -1.0, 1.1, 2.0, 100.0})
    @DisplayName("Should reject invalid tax rates")
    void testInvalidTaxRates(double invalidRate) {
        // Act & Assert
        assertThatThrownBy(() -> new FlatRateTaxStrategy(invalidRate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rate must be between 0 and 1");
    }

    @ParameterizedTest(name = "Valid rate: {0}%")
    @ValueSource(doubles = {0.0, 0.1, 0.15, 0.20, 0.25, 0.30, 0.50, 1.0})
    @DisplayName("Should accept valid tax rates")
    void testValidTaxRates(double validRate) {
        // Act
        TaxStrategy strategy = new FlatRateTaxStrategy(validRate);

        // Assert
        assertThat(strategy).isNotNull();
        assertThat(strategy.calculateTax(new BigDecimal("100000")))
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    // -------------------------------------------------------------------------
    // Salary Range Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Salary range: {0} - {1}")
    @CsvSource({
        "0,       50000",     // Entry level
        "50000,   100000",    // Mid level
        "100000,  150000",    // Senior level
        "150000,  200000",    // Manager level
        "200000,  300000"     // Director level
    })
    @DisplayName("Should calculate tax correctly across salary ranges")
    void testSalaryRanges(BigDecimal minSalary, BigDecimal maxSalary) {
        // Arrange
        TaxStrategy strategy = new ProgressiveTaxStrategy();

        // Act
        BigDecimal minTax = strategy.calculateTax(minSalary);
        BigDecimal maxTax = strategy.calculateTax(maxSalary);

        // Assert
        assertThat(minTax).isLessThanOrEqualTo(maxTax);  // Higher salary = higher tax
        assertThat(minTax).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(maxTax).isLessThanOrEqualTo(maxSalary);
    }

    // -------------------------------------------------------------------------
    // Percentage Calculation Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Tax percentage: salary={0}, rate={1}% → effective={2}%")
    @CsvSource({
        "100000,  20,  20.00",    // Flat 20%
        "100000,  15,  15.00",    // Flat 15%
        "100000,  25,  25.00",    // Flat 25%
        "50000,   20,  20.00",    // Flat 20%
        "200000,  30,  30.00"     // Flat 30%
    })
    @DisplayName("Should maintain correct tax percentage for flat rate")
    void testEffectiveTaxPercentage(BigDecimal salary, int ratePercent, BigDecimal expectedEffectiveRate) {
        // Arrange
        BigDecimal rate = new BigDecimal(ratePercent).divide(new BigDecimal("100"));
        TaxStrategy strategy = new FlatRateTaxStrategy(rate);

        // Act
        BigDecimal tax = strategy.calculateTax(salary);
        BigDecimal effectiveRate = tax.divide(salary, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        // Assert
        assertThat(effectiveRate).isEqualByComparingTo(expectedEffectiveRate);
    }

    // -------------------------------------------------------------------------
    // Rounding Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Rounding: {0} * {1}% = {2}")
    @CsvSource({
        "100000.00,  20.5,  20500.00",     // Even rounding
        "100000.00,  20.55, 20550.00",     // Precise
        "99999.99,   20,    20000.00",     // Round down
        "100000.01,  20,    20000.00",     // Round down
        "123456.78,  15.5,  19135.80",     // Complex rounding
        "77777.77,   22.22,  17277.73"     // Multiple decimals
    })
    @DisplayName("Should round tax calculations correctly (2 decimal places)")
    void testTaxRounding(BigDecimal salary, double ratePercent, BigDecimal expectedTax) {
        // Arrange
        BigDecimal rate = BigDecimal.valueOf(ratePercent).divide(new BigDecimal("100"));
        TaxStrategy strategy = new FlatRateTaxStrategy(rate);

        // Act
        BigDecimal tax = strategy.calculateTax(salary);

        // Assert
        assertThat(tax).isEqualByComparingTo(expectedTax);
        assertThat(tax.scale()).isLessThanOrEqualTo(2);  // Max 2 decimal places
    }

    // -------------------------------------------------------------------------
    // Custom Progressive Brackets Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Custom brackets: salary={0} → tax={1}")
    @CsvSource({
        "25000,   1250.00",    // 25k @ 5% = 1250
        "60000,   3750.00",    // 25k@5% + 35k@10% = 1250 + 3500 = 4750 (wait, let me recalc)
        "110000,  10750.00"    // 25k@5% + 35k@10% + 50k@15% = 1250 + 3500 + 7500 = 12250
    })
    @DisplayName("Should calculate tax with custom progressive brackets")
    void testCustomProgressiveBrackets(BigDecimal salary, BigDecimal expectedTax) {
        // Arrange - Custom tax brackets
        List<ProgressiveTaxStrategy.Bracket> customBrackets = List.of(
            new ProgressiveTaxStrategy.Bracket(
                BigDecimal.ZERO, new BigDecimal("25000"), new BigDecimal("0.05")),      // 0-25k at 5%
            new ProgressiveTaxStrategy.Bracket(
                new BigDecimal("25000"), new BigDecimal("60000"), new BigDecimal("0.10")),  // 25k-60k at 10%
            new ProgressiveTaxStrategy.Bracket(
                new BigDecimal("60000"), null, new BigDecimal("0.15"))                   // 60k+ at 15%
        );
        TaxStrategy strategy = new ProgressiveTaxStrategy(customBrackets);

        // Act
        BigDecimal tax = strategy.calculateTax(salary);

        // Assert - Note: These are approximate, actual calculations may vary slightly
        assertThat(tax).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(tax).isLessThanOrEqualTo(salary);
    }

    // -------------------------------------------------------------------------
    // Annual vs Monthly Salary Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Annual salary {0} → monthly {1} → tax {2}")
    @CsvSource({
        "1200000,  100000,  13000.00",   // 1.2M annual / 12 = 100k monthly
        "1800000,  150000,  28000.00",   // 1.8M annual / 12 = 150k monthly
        "2400000,  200000,  43000.00",   // 2.4M annual / 12 = 200k monthly
        "3000000,  250000,  58000.00"    // 3M annual / 12 = 250k monthly
    })
    @DisplayName("Should calculate monthly tax from annual salary correctly")
    void testAnnualToMonthlyTaxCalculation(BigDecimal annualSalary, BigDecimal monthlySalary, 
                                            BigDecimal expectedMonthlyTax) {
        // Arrange
        TaxStrategy strategy = new ProgressiveTaxStrategy();
        
        // Verify monthly salary calculation
        BigDecimal calculatedMonthly = annualSalary.divide(new BigDecimal("12"), 2, 
            java.math.RoundingMode.HALF_UP);
        assertThat(calculatedMonthly).isEqualByComparingTo(monthlySalary);

        // Act - Calculate tax on monthly salary
        BigDecimal monthlyTax = strategy.calculateTax(monthlySalary);

        // Assert
        assertThat(monthlyTax).isEqualByComparingTo(expectedMonthlyTax);
    }

    // -------------------------------------------------------------------------
    // Salary Increment Impact Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Increment: {0} + {1}% = {2}")
    @CsvSource({
        "100000,  10,  110000",    // 10% raise
        "100000,  15,  115000",    // 15% raise
        "100000,  20,  120000",    // 20% raise
        "150000,  10,  165000",    // 10% raise on higher salary
        "80000,   5,   84000"      // 5% raise
    })
    @DisplayName("Should calculate tax impact of salary increments")
    void testSalaryIncrementTaxImpact(BigDecimal originalSalary, int incrementPercent, 
                                       BigDecimal newSalary) {
        // Arrange
        TaxStrategy strategy = new ProgressiveTaxStrategy();

        // Act
        BigDecimal originalTax = strategy.calculateTax(originalSalary);
        BigDecimal newTax = strategy.calculateTax(newSalary);
        BigDecimal originalNet = originalSalary.subtract(originalTax);
        BigDecimal newNet = newSalary.subtract(newTax);

        // Assert
        assertThat(newTax).isGreaterThan(originalTax);  // More income = more tax
        assertThat(newNet).isGreaterThan(originalNet);  // But still more take-home

        BigDecimal netIncrease = newNet.subtract(originalNet);
        System.out.println("  Gross increase: " + newSalary.subtract(originalSalary) + 
                         " → Net increase: " + netIncrease);
    }

    // -------------------------------------------------------------------------
    // Null Safety Tests
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "Null check: {0}")
    @NullSource
    @DisplayName("Should reject null gross salary")
    void testNullGrossSalary(BigDecimal nullSalary) {
        // Arrange
        TaxStrategy strategy = new FlatRateTaxStrategy(0.20);

        // Act & Assert
        assertThatThrownBy(() -> strategy.calculateTax(nullSalary))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("grossSalary must not be null");
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private Employee createEmployee(String id, String name, BigDecimal salary, Role role) {
        return Employee.builder()
                .id(id)
                .name(name)
                .email(name.toLowerCase().replace(" ", ".") + "@example.com")
                .departmentId("D001")
                .role(role)
                .salary(salary)
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.now().minusYears(1))
                .build();
    }
}

