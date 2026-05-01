package com.srikanth.javareskill.domain;

import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the sealed class hierarchy:
 * <pre>
 * EmployeeType (sealed)
 * ├── PermanentEmployee (final)
 * └── ContractEmployee  (final)
 * </pre>
 *
 * <h2>Sealed-class features exercised</h2>
 * <ul>
 *   <li>Exhaustive pattern matching via {@code switch} (JDK 21+)</li>
 *   <li>Polymorphic dispatch of {@code annualCompensation()} and {@code typeName()}</li>
 *   <li>Subtype-specific fields and validation</li>
 *   <li>Compile-time restriction: only {@code PermanentEmployee} and
 *       {@code ContractEmployee} can extend {@code EmployeeType}</li>
 * </ul>
 */
@DisplayName("EmployeeType sealed hierarchy")
class EmployeeTypeTest {

    // -------------------------------------------------------------------------
    // Shared employee fixture
    // -------------------------------------------------------------------------

    private Employee baseEmployee;

    @BeforeEach
    void setUp() {
        baseEmployee = Employee.builder()
                .id("E001")
                .name("Alice Smith")
                .email("alice@example.com")
                .departmentId("D001")
                .role(Role.ENGINEER)
                .salary(new BigDecimal("10000"))   // monthly salary
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2022, 1, 1))
                .build();
    }

    // =========================================================================
    // PermanentEmployee
    // =========================================================================

    @Nested
    @DisplayName("PermanentEmployee")
    class PermanentEmployeeTests {

        @Nested
        @DisplayName("Construction")
        class Construction {

            @Test
            @DisplayName("stores employee, benefitsPercentage, and annualBonus")
            void allFieldsStored() {
                PermanentEmployee perm = new PermanentEmployee(
                        baseEmployee, new BigDecimal("0.15"), new BigDecimal("20000"));

                assertThat(perm.getEmployee()).isSameAs(baseEmployee);
                assertThat(perm.getBenefitsPercentage()).isEqualByComparingTo("0.15");
                assertThat(perm.getAnnualBonus()).isEqualByComparingTo("20000");
            }

            @Test
            @DisplayName("convenience delegates return employee fields")
            void convenienceDelegates() {
                PermanentEmployee perm = new PermanentEmployee(
                        baseEmployee, BigDecimal.ZERO, BigDecimal.ZERO);

                assertThat(perm.getId()).isEqualTo("E001");
                assertThat(perm.getName()).isEqualTo("Alice Smith");
                assertThat(perm.getEmail()).isEqualTo("alice@example.com");
                assertThat(perm.getSalary()).isEqualByComparingTo("10000");
            }

            @Test
            @DisplayName("typeName() returns 'Permanent'")
            void typeName() {
                PermanentEmployee perm = new PermanentEmployee(
                        baseEmployee, BigDecimal.ZERO, BigDecimal.ZERO);
                assertThat(perm.typeName()).isEqualTo("Permanent");
            }
        }

        @Nested
        @DisplayName("Validation")
        class Validation {

            @Test
            @DisplayName("null employee throws NullPointerException")
            void nullEmployee() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new PermanentEmployee(
                                null, BigDecimal.ZERO, BigDecimal.ZERO))
                        .withMessageContaining("employee");
            }

            @Test
            @DisplayName("null benefitsPercentage throws NullPointerException")
            void nullBenefitsPercentage() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new PermanentEmployee(
                                baseEmployee, null, BigDecimal.ZERO))
                        .withMessageContaining("benefitsPercentage");
            }

            @Test
            @DisplayName("null annualBonus throws NullPointerException")
            void nullAnnualBonus() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new PermanentEmployee(
                                baseEmployee, BigDecimal.ZERO, null))
                        .withMessageContaining("annualBonus");
            }

            @Test
            @DisplayName("negative benefitsPercentage throws IllegalArgumentException")
            void negativeBenefitsPercentage() {
                assertThatIllegalArgumentException()
                        .isThrownBy(() -> new PermanentEmployee(
                                baseEmployee, new BigDecimal("-0.01"), BigDecimal.ZERO));
            }

            @Test
            @DisplayName("benefitsPercentage > 1 throws IllegalArgumentException")
            void benefitsPercentageAboveOne() {
                assertThatIllegalArgumentException()
                        .isThrownBy(() -> new PermanentEmployee(
                                baseEmployee, new BigDecimal("1.01"), BigDecimal.ZERO));
            }

            @Test
            @DisplayName("negative annualBonus throws IllegalArgumentException")
            void negativeAnnualBonus() {
                assertThatIllegalArgumentException()
                        .isThrownBy(() -> new PermanentEmployee(
                                baseEmployee, BigDecimal.ZERO, new BigDecimal("-1")));
            }

            @Test
            @DisplayName("benefitsPercentage = 0 is allowed")
            void zeroBenefits() {
                assertThatNoException()
                        .isThrownBy(() -> new PermanentEmployee(
                                baseEmployee, BigDecimal.ZERO, BigDecimal.ZERO));
            }

            @Test
            @DisplayName("benefitsPercentage = 1 (100%) is allowed")
            void fullBenefits() {
                assertThatNoException()
                        .isThrownBy(() -> new PermanentEmployee(
                                baseEmployee, BigDecimal.ONE, BigDecimal.ZERO));
            }
        }

        @Nested
        @DisplayName("annualCompensation()")
        class AnnualCompensation {

            @Test
            @DisplayName("salary 10000, 15% benefits, 20000 bonus → 158000.00")
            void withBenefitsAndBonus() {
                // annual salary = 10000 × 12 = 120000
                // benefits = 120000 × 0.15 = 18000
                // total = 120000 + 18000 + 20000 = 158000
                PermanentEmployee perm = new PermanentEmployee(
                        baseEmployee, new BigDecimal("0.15"), new BigDecimal("20000"));

                assertThat(perm.annualCompensation()).isEqualByComparingTo("158000.00");
            }

            @Test
            @DisplayName("zero benefits and zero bonus → 12 × salary")
            void noBenefitsNoBonus() {
                PermanentEmployee perm = new PermanentEmployee(
                        baseEmployee, BigDecimal.ZERO, BigDecimal.ZERO);

                assertThat(perm.annualCompensation()).isEqualByComparingTo("120000.00");
            }

            @Test
            @DisplayName("result is rounded to 2 decimal places")
            void scaledToTwoDecimals() {
                // 10000 × 12 = 120000; 120000 × 0.07 = 8400; total = 128400.00
                PermanentEmployee perm = new PermanentEmployee(
                        baseEmployee, new BigDecimal("0.07"), BigDecimal.ZERO);

                assertThat(perm.annualCompensation().scale()).isEqualTo(2);
            }
        }

        @Nested
        @DisplayName("equals / hashCode / toString")
        class EqualsHashCodeToString {

            @Test
            @DisplayName("same employee ID → equal regardless of benefits")
            void sameIdEqual() {
                PermanentEmployee a = new PermanentEmployee(
                        baseEmployee, BigDecimal.ZERO, BigDecimal.ZERO);
                PermanentEmployee b = new PermanentEmployee(
                        baseEmployee, new BigDecimal("0.50"), new BigDecimal("99999"));

                assertThat(a).isEqualTo(b);
                assertThat(a.hashCode()).isEqualTo(b.hashCode());
            }

            @Test
            @DisplayName("toString contains 'Permanent' and employee info")
            void toStringContents() {
                PermanentEmployee perm = new PermanentEmployee(
                        baseEmployee, new BigDecimal("0.15"), new BigDecimal("20000"));

                assertThat(perm.toString())
                        .contains("Permanent", "E001", "0.15", "20000");
            }
        }
    }

    // =========================================================================
    // ContractEmployee
    // =========================================================================

    @Nested
    @DisplayName("ContractEmployee")
    class ContractEmployeeTests {

        private static final LocalDate END_DATE = LocalDate.of(2027, 12, 31);

        @Nested
        @DisplayName("Construction")
        class Construction {

            @Test
            @DisplayName("stores employee, contractEndDate, hourlyRate, contractedHours")
            void allFieldsStored() {
                ContractEmployee contract = new ContractEmployee(
                        baseEmployee, END_DATE, new BigDecimal("75"), 2000);

                assertThat(contract.getEmployee()).isSameAs(baseEmployee);
                assertThat(contract.getContractEndDate()).isEqualTo(END_DATE);
                assertThat(contract.getHourlyRate()).isEqualByComparingTo("75");
                assertThat(contract.getContractedHoursPerYear()).isEqualTo(2000);
            }

            @Test
            @DisplayName("typeName() returns 'Contract'")
            void typeName() {
                ContractEmployee c = new ContractEmployee(
                        baseEmployee, END_DATE, new BigDecimal("50"), 1000);
                assertThat(c.typeName()).isEqualTo("Contract");
            }
        }

        @Nested
        @DisplayName("Validation")
        class Validation {

            @Test
            @DisplayName("null employee throws NullPointerException")
            void nullEmployee() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new ContractEmployee(
                                null, END_DATE, BigDecimal.TEN, 100))
                        .withMessageContaining("employee");
            }

            @Test
            @DisplayName("null contractEndDate throws NullPointerException")
            void nullContractEndDate() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new ContractEmployee(
                                baseEmployee, null, BigDecimal.TEN, 100))
                        .withMessageContaining("contractEndDate");
            }

            @Test
            @DisplayName("null hourlyRate throws NullPointerException")
            void nullHourlyRate() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new ContractEmployee(
                                baseEmployee, END_DATE, null, 100))
                        .withMessageContaining("hourlyRate");
            }

            @Test
            @DisplayName("negative hourlyRate throws IllegalArgumentException")
            void negativeHourlyRate() {
                assertThatIllegalArgumentException()
                        .isThrownBy(() -> new ContractEmployee(
                                baseEmployee, END_DATE, new BigDecimal("-1"), 100));
            }

            @Test
            @DisplayName("zero contractedHoursPerYear throws IllegalArgumentException")
            void zeroHours() {
                assertThatIllegalArgumentException()
                        .isThrownBy(() -> new ContractEmployee(
                                baseEmployee, END_DATE, BigDecimal.TEN, 0));
            }

            @Test
            @DisplayName("negative contractedHoursPerYear throws IllegalArgumentException")
            void negativeHours() {
                assertThatIllegalArgumentException()
                        .isThrownBy(() -> new ContractEmployee(
                                baseEmployee, END_DATE, BigDecimal.TEN, -1));
            }
        }

        @Nested
        @DisplayName("annualCompensation()")
        class AnnualCompensation {

            @Test
            @DisplayName("hourlyRate 75 × 2000 hours = 150000.00")
            void correctCalculation() {
                ContractEmployee c = new ContractEmployee(
                        baseEmployee, END_DATE, new BigDecimal("75"), 2000);

                assertThat(c.annualCompensation()).isEqualByComparingTo("150000.00");
            }

            @Test
            @DisplayName("result is rounded to 2 decimal places")
            void scaledToTwoDecimals() {
                ContractEmployee c = new ContractEmployee(
                        baseEmployee, END_DATE, new BigDecimal("33.33"), 1000);

                assertThat(c.annualCompensation().scale()).isEqualTo(2);
            }
        }

        @Nested
        @DisplayName("isExpired()")
        class IsExpired {

            @Test
            @DisplayName("returns false when asOf is before contractEndDate")
            void beforeEndDate_notExpired() {
                ContractEmployee c = new ContractEmployee(
                        baseEmployee, END_DATE, BigDecimal.TEN, 100);

                assertThat(c.isExpired(END_DATE.minusDays(1))).isFalse();
            }

            @Test
            @DisplayName("returns false when asOf equals contractEndDate (not strictly before)")
            void onEndDate_notExpired() {
                ContractEmployee c = new ContractEmployee(
                        baseEmployee, END_DATE, BigDecimal.TEN, 100);

                assertThat(c.isExpired(END_DATE)).isFalse();
            }

            @Test
            @DisplayName("returns true when asOf is after contractEndDate")
            void afterEndDate_expired() {
                ContractEmployee c = new ContractEmployee(
                        baseEmployee, END_DATE, BigDecimal.TEN, 100);

                assertThat(c.isExpired(END_DATE.plusDays(1))).isTrue();
            }
        }

        @Nested
        @DisplayName("equals / hashCode / toString")
        class EqualsHashCodeToString {

            @Test
            @DisplayName("same employee ID → equal regardless of contract details")
            void sameIdEqual() {
                ContractEmployee a = new ContractEmployee(
                        baseEmployee, END_DATE, new BigDecimal("50"), 1000);
                ContractEmployee b = new ContractEmployee(
                        baseEmployee, END_DATE.plusYears(1), new BigDecimal("100"), 2000);

                assertThat(a).isEqualTo(b);
                assertThat(a.hashCode()).isEqualTo(b.hashCode());
            }

            @Test
            @DisplayName("toString contains 'Contract' and employee info")
            void toStringContents() {
                ContractEmployee c = new ContractEmployee(
                        baseEmployee, END_DATE, new BigDecimal("75"), 2000);

                assertThat(c.toString())
                        .contains("Contract", "E001", "75", "2000");
            }
        }
    }

    // =========================================================================
    // Sealed hierarchy — polymorphism & pattern matching
    // =========================================================================

    @Nested
    @DisplayName("Sealed hierarchy polymorphism")
    class SealedHierarchyTests {

        @Test
        @DisplayName("PermanentEmployee is an instance of EmployeeType")
        void permanentIsEmployeeType() {
            EmployeeType type = new PermanentEmployee(
                    baseEmployee, BigDecimal.ZERO, BigDecimal.ZERO);
            assertThat(type).isInstanceOf(EmployeeType.class);
        }

        @Test
        @DisplayName("ContractEmployee is an instance of EmployeeType")
        void contractIsEmployeeType() {
            EmployeeType type = new ContractEmployee(
                    baseEmployee, LocalDate.of(2027, 12, 31), BigDecimal.TEN, 100);
            assertThat(type).isInstanceOf(EmployeeType.class);
        }

        @Test
        @DisplayName("annualCompensation() dispatches polymorphically")
        void polymorphicDispatch() {
            EmployeeType permanent = new PermanentEmployee(
                    baseEmployee, new BigDecimal("0.10"), new BigDecimal("5000"));
            EmployeeType contract = new ContractEmployee(
                    baseEmployee, LocalDate.of(2027, 12, 31),
                    new BigDecimal("60"), 2000);

            // Permanent: 120000 + 12000 + 5000 = 137000
            assertThat(permanent.annualCompensation()).isEqualByComparingTo("137000.00");
            // Contract: 60 × 2000 = 120000
            assertThat(contract.annualCompensation()).isEqualByComparingTo("120000.00");
        }

        @Test
        @DisplayName("exhaustive pattern matching with switch (sealed guarantee)")
        void exhaustivePatternMatching() {
            EmployeeType perm = new PermanentEmployee(
                    baseEmployee, new BigDecimal("0.10"), BigDecimal.ZERO);
            EmployeeType cont = new ContractEmployee(
                    baseEmployee, LocalDate.of(2027, 12, 31), new BigDecimal("50"), 1000);

            // The switch is exhaustive because EmployeeType is sealed with
            // exactly two permitted subtypes — no default branch needed.
            String permLabel = describeType(perm);
            String contLabel = describeType(cont);

            assertThat(permLabel).isEqualTo("Permanent employee with 10% benefits");
            assertThat(contLabel).isEqualTo("Contractor until 2027-12-31");
        }

        @Test
        @DisplayName("PermanentEmployee and ContractEmployee with same ID are equal (Liskov)")
        void crossSubtypeEquality() {
            PermanentEmployee perm = new PermanentEmployee(
                    baseEmployee, BigDecimal.ZERO, BigDecimal.ZERO);
            ContractEmployee cont = new ContractEmployee(
                    baseEmployee, LocalDate.of(2027, 12, 31), BigDecimal.TEN, 100);

            // Both wrap the same Employee (E001) → equal via EmployeeType.equals()
            assertThat((EmployeeType) perm).isEqualTo(cont);
        }

        @Test
        @DisplayName("typeName() returns distinct labels for each subtype")
        void typeNameDistinct() {
            EmployeeType perm = new PermanentEmployee(
                    baseEmployee, BigDecimal.ZERO, BigDecimal.ZERO);
            EmployeeType cont = new ContractEmployee(
                    baseEmployee, LocalDate.of(2027, 12, 31), BigDecimal.TEN, 100);

            assertThat(perm.typeName()).isEqualTo("Permanent");
            assertThat(cont.typeName()).isEqualTo("Contract");
            assertThat(perm.typeName()).isNotEqualTo(cont.typeName());
        }

        // -----------------------------------------------------------------
        // Helper — demonstrates JDK 17 pattern matching with instanceof
        // (sealed-class switch is a JDK 21+ feature; instanceof works on 17)
        // -----------------------------------------------------------------

        private String describeType(EmployeeType type) {
            if (type instanceof PermanentEmployee p) {
                return "Permanent employee with "
                        + p.getBenefitsPercentage()
                            .multiply(BigDecimal.valueOf(100))
                            .stripTrailingZeros().toPlainString()
                        + "% benefits";
            } else if (type instanceof ContractEmployee c) {
                return "Contractor until " + c.getContractEndDate();
            }
            // Unreachable for a sealed hierarchy — compiler guarantees completeness
            throw new AssertionError("Unknown EmployeeType: " + type.getClass());
        }
    }
}

