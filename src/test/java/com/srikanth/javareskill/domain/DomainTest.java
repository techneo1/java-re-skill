package com.srikanth.javareskill.domain;

import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Nested unit tests for the three core domain objects:
 * {@link Employee}, {@link Department}, and {@link PayrollRecord}.
 *
 * <h2>Test organisation</h2>
 * Each domain class gets its own top-level {@code @Nested} class, which is
 * further subdivided into scenario groups:
 * <ul>
 *   <li><b>Construction</b>   – happy-path builder usage; all fields accessible</li>
 *   <li><b>Validation</b>     – null-field guards and business-rule constraints</li>
 *   <li><b>Equality</b>       – equals / hashCode contract (reflexive, symmetric,
 *                               transitive, null-safe, type-safe)</li>
 *   <li><b>ToString</b>       – toString contains the identifying fields</li>
 * </ul>
 *
 * <h2>JUnit 5 features used</h2>
 * <ul>
 *   <li>{@code @Nested}       – groups related scenarios under a shared fixture</li>
 *   <li>{@code @BeforeEach}   – sets up shared test state once per nested class</li>
 *   <li>{@code @DisplayName}  – human-readable names in test reports</li>
 * </ul>
 */
@DisplayName("Domain model")
class DomainTest {

    // =========================================================================
    // Employee
    // =========================================================================

    @Nested
    @DisplayName("Employee")
    class EmployeeTests {

        // ------------------------------------------------------------------
        // Shared fixture available to every test in this nested class
        // ------------------------------------------------------------------

        private Employee alice;

        @BeforeEach
        void setUp() {
            alice = Employee.builder()
                    .id("E001")
                    .name("Alice Smith")
                    .email("alice@example.com")
                    .departmentId("D001")
                    .role(Role.ENGINEER)
                    .salary(new BigDecimal("75000.00"))
                    .status(EmployeeStatus.ACTIVE)
                    .joiningDate(LocalDate.of(2022, 6, 1))
                    .build();
        }

        // ------------------------------------------------------------------
        // Construction
        // ------------------------------------------------------------------

        @Nested
        @DisplayName("Construction")
        class Construction {

            @Test
            @DisplayName("all fields are stored exactly as supplied")
            void allFieldsStoredCorrectly() {
                assertThat(alice.getId())           .isEqualTo("E001");
                assertThat(alice.getName())         .isEqualTo("Alice Smith");
                assertThat(alice.getEmail())        .isEqualTo("alice@example.com");
                assertThat(alice.getDepartmentId()) .isEqualTo("D001");
                assertThat(alice.getRole())         .isEqualTo(Role.ENGINEER);
                assertThat(alice.getSalary())       .isEqualByComparingTo("75000.00");
                assertThat(alice.getStatus())       .isEqualTo(EmployeeStatus.ACTIVE);
                assertThat(alice.getJoiningDate())  .isEqualTo(LocalDate.of(2022, 6, 1));
            }

            @Test
            @DisplayName("zero salary is allowed (volunteer / unpaid role)")
            void zeroSalaryIsAllowed() {
                Employee volunteer = Employee.builder()
                        .id("E002").name("Bob").email("bob@example.com")
                        .departmentId("D001").role(Role.HR).salary(BigDecimal.ZERO)
                        .status(EmployeeStatus.ACTIVE).joiningDate(LocalDate.now())
                        .build();

                assertThat(volunteer.getSalary()).isEqualByComparingTo("0");
            }

            @Test
            @DisplayName("INACTIVE status is stored correctly")
            void inactiveStatusStored() {
                Employee inactive = Employee.builder()
                        .id("E003").name("Carol").email("carol@example.com")
                        .departmentId("D002").role(Role.ANALYST)
                        .salary(new BigDecimal("50000"))
                        .status(EmployeeStatus.INACTIVE).joiningDate(LocalDate.now())
                        .build();

                assertThat(inactive.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
            }
        }

        // ------------------------------------------------------------------
        // Validation
        // ------------------------------------------------------------------

        @Nested
        @DisplayName("Validation")
        class Validation {

            @Test
            @DisplayName("null id throws NullPointerException with 'id' in the message")
            void nullId_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> Employee.builder()
                                .name("X").email("x@x.com").departmentId("D1")
                                .role(Role.HR).salary(BigDecimal.TEN)
                                .status(EmployeeStatus.ACTIVE).joiningDate(LocalDate.now())
                                .build())
                        .withMessageContaining("id");
            }

            @Test
            @DisplayName("null name throws NullPointerException with 'name' in the message")
            void nullName_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> Employee.builder()
                                .id("E001").email("x@x.com").departmentId("D1")
                                .role(Role.HR).salary(BigDecimal.TEN)
                                .status(EmployeeStatus.ACTIVE).joiningDate(LocalDate.now())
                                .build())
                        .withMessageContaining("name");
            }

            @Test
            @DisplayName("null email throws NullPointerException with 'email' in the message")
            void nullEmail_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> Employee.builder()
                                .id("E001").name("X").departmentId("D1")
                                .role(Role.HR).salary(BigDecimal.TEN)
                                .status(EmployeeStatus.ACTIVE).joiningDate(LocalDate.now())
                                .build())
                        .withMessageContaining("email");
            }

            @Test
            @DisplayName("null departmentId throws NullPointerException")
            void nullDepartmentId_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> Employee.builder()
                                .id("E001").name("X").email("x@x.com")
                                .role(Role.HR).salary(BigDecimal.TEN)
                                .status(EmployeeStatus.ACTIVE).joiningDate(LocalDate.now())
                                .build())
                        .withMessageContaining("departmentId");
            }

            @Test
            @DisplayName("null role throws NullPointerException")
            void nullRole_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> Employee.builder()
                                .id("E001").name("X").email("x@x.com").departmentId("D1")
                                .salary(BigDecimal.TEN)
                                .status(EmployeeStatus.ACTIVE).joiningDate(LocalDate.now())
                                .build())
                        .withMessageContaining("role");
            }

            @Test
            @DisplayName("null salary throws NullPointerException")
            void nullSalary_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> Employee.builder()
                                .id("E001").name("X").email("x@x.com").departmentId("D1")
                                .role(Role.HR)
                                .status(EmployeeStatus.ACTIVE).joiningDate(LocalDate.now())
                                .build())
                        .withMessageContaining("salary");
            }

            @Test
            @DisplayName("null status throws NullPointerException")
            void nullStatus_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> Employee.builder()
                                .id("E001").name("X").email("x@x.com").departmentId("D1")
                                .role(Role.HR).salary(BigDecimal.TEN)
                                .joiningDate(LocalDate.now())
                                .build())
                        .withMessageContaining("status");
            }

            @Test
            @DisplayName("null joiningDate throws NullPointerException")
            void nullJoiningDate_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> Employee.builder()
                                .id("E001").name("X").email("x@x.com").departmentId("D1")
                                .role(Role.HR).salary(BigDecimal.TEN)
                                .status(EmployeeStatus.ACTIVE)
                                .build())
                        .withMessageContaining("joiningDate");
            }

            @Test
            @DisplayName("negative salary throws IllegalArgumentException")
            void negativeSalary_throwsIllegalArgumentException() {
                assertThatIllegalArgumentException()
                        .isThrownBy(() -> Employee.builder()
                                .id("E001").name("X").email("x@x.com").departmentId("D1")
                                .role(Role.HR).salary(new BigDecimal("-0.01"))
                                .status(EmployeeStatus.ACTIVE).joiningDate(LocalDate.now())
                                .build());
            }
        }

        // ------------------------------------------------------------------
        // Equality & HashCode
        // ------------------------------------------------------------------

        @Nested
        @DisplayName("Equality and HashCode")
        class EqualityAndHashCode {

            @Test
            @DisplayName("same ID → equal (identity-based equality)")
            void sameId_areEqual() {
                Employee other = Employee.builder()
                        .id("E001").name("Alice Duplicate").email("dup@example.com")
                        .departmentId("D999").role(Role.DIRECTOR)
                        .salary(new BigDecimal("999999")).status(EmployeeStatus.INACTIVE)
                        .joiningDate(LocalDate.of(2000, 1, 1))
                        .build();

                assertThat(alice).isEqualTo(other);
            }

            @Test
            @DisplayName("different ID → not equal")
            void differentId_notEqual() {
                Employee bob = Employee.builder()
                        .id("E002").name("Bob").email("bob@example.com")
                        .departmentId("D001").role(Role.ENGINEER)
                        .salary(new BigDecimal("75000")).status(EmployeeStatus.ACTIVE)
                        .joiningDate(LocalDate.of(2022, 6, 1))
                        .build();

                assertThat(alice).isNotEqualTo(bob);
            }

            @Test
            @DisplayName("reflexive: employee equals itself")
            void reflexive() {
                assertThat(alice).isEqualTo(alice);
            }

            @Test
            @DisplayName("symmetric: a.equals(b) ↔ b.equals(a)")
            void symmetric() {
                Employee clone = Employee.builder()
                        .id("E001").name("Clone").email("c@c.com").departmentId("D1")
                        .role(Role.HR).salary(BigDecimal.TEN).status(EmployeeStatus.ACTIVE)
                        .joiningDate(LocalDate.now()).build();

                assertThat(alice).isEqualTo(clone);
                assertThat(clone).isEqualTo(alice);
            }

            @Test
            @DisplayName("not equal to null")
            void notEqualToNull() {
                assertThat(alice).isNotEqualTo(null);
            }

            @Test
            @DisplayName("not equal to a different type")
            void notEqualToDifferentType() {
                assertThat(alice).isNotEqualTo("E001");
            }

            @Test
            @DisplayName("equal employees have the same hashCode")
            void equalObjects_sameHashCode() {
                Employee clone = Employee.builder()
                        .id("E001").name("Clone").email("c@c.com").departmentId("D1")
                        .role(Role.HR).salary(BigDecimal.TEN).status(EmployeeStatus.ACTIVE)
                        .joiningDate(LocalDate.now()).build();

                assertThat(alice.hashCode()).isEqualTo(clone.hashCode());
            }

            @Test
            @DisplayName("hashCode is stable across multiple calls")
            void hashCode_stable() {
                int first = alice.hashCode();
                assertThat(alice.hashCode()).isEqualTo(first);
                assertThat(alice.hashCode()).isEqualTo(first);
            }
        }

        // ------------------------------------------------------------------
        // ToString
        // ------------------------------------------------------------------

        @Nested
        @DisplayName("toString")
        class ToStringTests {

            @Test
            @DisplayName("contains the employee's id")
            void containsId() {
                assertThat(alice.toString()).contains("E001");
            }

            @Test
            @DisplayName("contains the employee's name")
            void containsName() {
                assertThat(alice.toString()).contains("Alice Smith");
            }

            @Test
            @DisplayName("contains the employee's email")
            void containsEmail() {
                assertThat(alice.toString()).contains("alice@example.com");
            }

            @Test
            @DisplayName("contains the role")
            void containsRole() {
                assertThat(alice.toString()).contains("ENGINEER");
            }
        }
    }

    // =========================================================================
    // Department
    // =========================================================================

    @Nested
    @DisplayName("Department")
    class DepartmentTests {

        private Department engineering;

        @BeforeEach
        void setUp() {
            engineering = Department.builder()
                    .id("D001")
                    .name("Engineering")
                    .location("New York")
                    .build();
        }

        // ------------------------------------------------------------------
        // Construction
        // ------------------------------------------------------------------

        @Nested
        @DisplayName("Construction")
        class Construction {

            @Test
            @DisplayName("all fields are stored exactly as supplied")
            void allFieldsStoredCorrectly() {
                assertThat(engineering.getId())       .isEqualTo("D001");
                assertThat(engineering.getName())     .isEqualTo("Engineering");
                assertThat(engineering.getLocation()) .isEqualTo("New York");
            }
        }

        // ------------------------------------------------------------------
        // Validation
        // ------------------------------------------------------------------

        @Nested
        @DisplayName("Validation")
        class Validation {

            @Test
            @DisplayName("null id throws NullPointerException with 'id' in the message")
            void nullId_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> Department.builder().name("HR").location("London").build())
                        .withMessageContaining("id");
            }

            @Test
            @DisplayName("null name throws NullPointerException with 'name' in the message")
            void nullName_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> Department.builder().id("D002").location("London").build())
                        .withMessageContaining("name");
            }

            @Test
            @DisplayName("null location throws NullPointerException with 'location' in the message")
            void nullLocation_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> Department.builder().id("D002").name("HR").build())
                        .withMessageContaining("location");
            }
        }

        // ------------------------------------------------------------------
        // Equality & HashCode
        // ------------------------------------------------------------------

        @Nested
        @DisplayName("Equality and HashCode")
        class EqualityAndHashCode {

            @Test
            @DisplayName("same ID → equal regardless of other field values")
            void sameId_areEqual() {
                Department other = Department.builder()
                        .id("D001").name("Different Name").location("Different City").build();

                assertThat(engineering).isEqualTo(other);
            }

            @Test
            @DisplayName("different ID → not equal")
            void differentId_notEqual() {
                Department hr = Department.builder().id("D002").name("HR").location("Boston").build();
                assertThat(engineering).isNotEqualTo(hr);
            }

            @Test
            @DisplayName("reflexive: department equals itself")
            void reflexive() {
                assertThat(engineering).isEqualTo(engineering);
            }

            @Test
            @DisplayName("symmetric: a.equals(b) ↔ b.equals(a)")
            void symmetric() {
                Department twin = Department.builder().id("D001").name("Eng2").location("LA").build();
                assertThat(engineering).isEqualTo(twin);
                assertThat(twin).isEqualTo(engineering);
            }

            @Test
            @DisplayName("not equal to null")
            void notEqualToNull() {
                assertThat(engineering).isNotEqualTo(null);
            }

            @Test
            @DisplayName("not equal to a different type")
            void notEqualToDifferentType() {
                assertThat(engineering).isNotEqualTo("D001");
            }

            @Test
            @DisplayName("equal departments have the same hashCode")
            void equalObjects_sameHashCode() {
                Department twin = Department.builder().id("D001").name("Eng2").location("LA").build();
                assertThat(engineering.hashCode()).isEqualTo(twin.hashCode());
            }

            @Test
            @DisplayName("hashCode is stable across multiple calls")
            void hashCode_stable() {
                int first = engineering.hashCode();
                assertThat(engineering.hashCode()).isEqualTo(first);
            }
        }

        // ------------------------------------------------------------------
        // ToString
        // ------------------------------------------------------------------

        @Nested
        @DisplayName("toString")
        class ToStringTests {

            @Test
            @DisplayName("contains the department id")
            void containsId() {
                assertThat(engineering.toString()).contains("D001");
            }

            @Test
            @DisplayName("contains the department name")
            void containsName() {
                assertThat(engineering.toString()).contains("Engineering");
            }

            @Test
            @DisplayName("contains the location")
            void containsLocation() {
                assertThat(engineering.toString()).contains("New York");
            }
        }
    }

    // =========================================================================
    // PayrollRecord
    // =========================================================================

    @Nested
    @DisplayName("PayrollRecord")
    class PayrollRecordTests {

        private static final LocalDate APRIL_2026         = LocalDate.of(2026, 4, 1);
        private static final LocalDateTime PROCESSED_TS   = LocalDateTime.of(2026, 4, 30, 12, 0);

        private PayrollRecord record;

        @BeforeEach
        void setUp() {
            record = PayrollRecord.builder()
                    .id("PR001")
                    .employeeId("E001")
                    .grossSalary(new BigDecimal("100000.00"))
                    .taxAmount(new BigDecimal("20000.00"))
                    .payrollMonth(APRIL_2026)
                    .processedTimestamp(PROCESSED_TS)
                    .build();
        }

        // ------------------------------------------------------------------
        // Construction
        // ------------------------------------------------------------------

        @Nested
        @DisplayName("Construction")
        class Construction {

            @Test
            @DisplayName("all supplied fields are stored correctly")
            void allFieldsStoredCorrectly() {
                assertThat(record.getId())                  .isEqualTo("PR001");
                assertThat(record.getEmployeeId())          .isEqualTo("E001");
                assertThat(record.getGrossSalary())         .isEqualByComparingTo("100000.00");
                assertThat(record.getTaxAmount())           .isEqualByComparingTo("20000.00");
                assertThat(record.getPayrollMonth())        .isEqualTo(APRIL_2026);
                assertThat(record.getProcessedTimestamp())  .isEqualTo(PROCESSED_TS);
            }

            @Test
            @DisplayName("netSalary is derived as grossSalary − taxAmount")
            void netSalaryDerivedCorrectly() {
                assertThat(record.getNetSalary()).isEqualByComparingTo("80000.00");
            }

            @Test
            @DisplayName("zero tax → netSalary equals grossSalary")
            void zeroTax_netEqualGross() {
                PayrollRecord noTax = PayrollRecord.builder()
                        .id("PR002").employeeId("E001")
                        .grossSalary(new BigDecimal("50000"))
                        .taxAmount(BigDecimal.ZERO)
                        .payrollMonth(APRIL_2026).processedTimestamp(PROCESSED_TS)
                        .build();

                assertThat(noTax.getNetSalary()).isEqualByComparingTo("50000");
            }

            @Test
            @DisplayName("tax equal to gross → netSalary is zero")
            void taxEqualsGross_netIsZero() {
                PayrollRecord fullTax = PayrollRecord.builder()
                        .id("PR003").employeeId("E001")
                        .grossSalary(new BigDecimal("40000"))
                        .taxAmount(new BigDecimal("40000"))
                        .payrollMonth(APRIL_2026).processedTimestamp(PROCESSED_TS)
                        .build();

                assertThat(fullTax.getNetSalary()).isEqualByComparingTo("0");
            }

            @Test
            @DisplayName("netSalary invariant: net + tax == gross for any valid record")
            void netPlusTaxEqualsGross_invariant() {
                BigDecimal expectedGross = record.getNetSalary().add(record.getTaxAmount());
                assertThat(expectedGross).isEqualByComparingTo(record.getGrossSalary());
            }
        }

        // ------------------------------------------------------------------
        // Validation
        // ------------------------------------------------------------------

        @Nested
        @DisplayName("Validation")
        class Validation {

            @Test
            @DisplayName("null id throws NullPointerException")
            void nullId_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> PayrollRecord.builder()
                                .employeeId("E001")
                                .grossSalary(new BigDecimal("50000"))
                                .taxAmount(BigDecimal.ZERO)
                                .payrollMonth(APRIL_2026).processedTimestamp(PROCESSED_TS)
                                .build())
                        .withMessageContaining("id");
            }

            @Test
            @DisplayName("null employeeId throws NullPointerException")
            void nullEmployeeId_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> PayrollRecord.builder()
                                .id("PR001")
                                .grossSalary(new BigDecimal("50000"))
                                .taxAmount(BigDecimal.ZERO)
                                .payrollMonth(APRIL_2026).processedTimestamp(PROCESSED_TS)
                                .build())
                        .withMessageContaining("employeeId");
            }

            @Test
            @DisplayName("null grossSalary throws NullPointerException")
            void nullGrossSalary_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> PayrollRecord.builder()
                                .id("PR001").employeeId("E001")
                                .taxAmount(BigDecimal.ZERO)
                                .payrollMonth(APRIL_2026).processedTimestamp(PROCESSED_TS)
                                .build())
                        .withMessageContaining("grossSalary");
            }

            @Test
            @DisplayName("null taxAmount throws NullPointerException")
            void nullTaxAmount_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> PayrollRecord.builder()
                                .id("PR001").employeeId("E001")
                                .grossSalary(new BigDecimal("50000"))
                                .payrollMonth(APRIL_2026).processedTimestamp(PROCESSED_TS)
                                .build())
                        .withMessageContaining("taxAmount");
            }

            @Test
            @DisplayName("negative grossSalary throws IllegalArgumentException")
            void negativeGrossSalary_throwsIllegalArgumentException() {
                assertThatIllegalArgumentException()
                        .isThrownBy(() -> PayrollRecord.builder()
                                .id("PR001").employeeId("E001")
                                .grossSalary(new BigDecimal("-1"))
                                .taxAmount(BigDecimal.ZERO)
                                .payrollMonth(APRIL_2026).processedTimestamp(PROCESSED_TS)
                                .build());
            }

            @Test
            @DisplayName("negative taxAmount throws IllegalArgumentException")
            void negativeTax_throwsIllegalArgumentException() {
                assertThatIllegalArgumentException()
                        .isThrownBy(() -> PayrollRecord.builder()
                                .id("PR001").employeeId("E001")
                                .grossSalary(new BigDecimal("50000"))
                                .taxAmount(new BigDecimal("-1"))
                                .payrollMonth(APRIL_2026).processedTimestamp(PROCESSED_TS)
                                .build());
            }

            @Test
            @DisplayName("taxAmount exceeding grossSalary throws IllegalArgumentException")
            void taxExceedsGross_throwsIllegalArgumentException() {
                assertThatIllegalArgumentException()
                        .isThrownBy(() -> PayrollRecord.builder()
                                .id("PR001").employeeId("E001")
                                .grossSalary(new BigDecimal("5000"))
                                .taxAmount(new BigDecimal("5001"))
                                .payrollMonth(APRIL_2026).processedTimestamp(PROCESSED_TS)
                                .build());
            }

            @Test
            @DisplayName("null payrollMonth throws NullPointerException")
            void nullPayrollMonth_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> PayrollRecord.builder()
                                .id("PR001").employeeId("E001")
                                .grossSalary(new BigDecimal("50000"))
                                .taxAmount(BigDecimal.ZERO)
                                .processedTimestamp(PROCESSED_TS)
                                .build())
                        .withMessageContaining("payrollMonth");
            }

            @Test
            @DisplayName("null processedTimestamp throws NullPointerException")
            void nullProcessedTimestamp_throwsNpe() {
                assertThatNullPointerException()
                        .isThrownBy(() -> PayrollRecord.builder()
                                .id("PR001").employeeId("E001")
                                .grossSalary(new BigDecimal("50000"))
                                .taxAmount(BigDecimal.ZERO)
                                .payrollMonth(APRIL_2026)
                                .build())
                        .withMessageContaining("processedTimestamp");
            }
        }

        // ------------------------------------------------------------------
        // Equality & HashCode
        // ------------------------------------------------------------------

        @Nested
        @DisplayName("Equality and HashCode")
        class EqualityAndHashCode {

            @Test
            @DisplayName("same ID → equal regardless of financial fields")
            void sameId_areEqual() {
                PayrollRecord other = PayrollRecord.builder()
                        .id("PR001").employeeId("E999")
                        .grossSalary(new BigDecimal("1"))
                        .taxAmount(BigDecimal.ZERO)
                        .payrollMonth(APRIL_2026).processedTimestamp(PROCESSED_TS)
                        .build();

                assertThat(record).isEqualTo(other);
            }

            @Test
            @DisplayName("different ID → not equal")
            void differentId_notEqual() {
                PayrollRecord other = PayrollRecord.builder()
                        .id("PR002").employeeId("E001")
                        .grossSalary(new BigDecimal("100000"))
                        .taxAmount(new BigDecimal("20000"))
                        .payrollMonth(APRIL_2026).processedTimestamp(PROCESSED_TS)
                        .build();

                assertThat(record).isNotEqualTo(other);
            }

            @Test
            @DisplayName("reflexive: record equals itself")
            void reflexive() {
                assertThat(record).isEqualTo(record);
            }

            @Test
            @DisplayName("not equal to null")
            void notEqualToNull() {
                assertThat(record).isNotEqualTo(null);
            }

            @Test
            @DisplayName("not equal to a different type")
            void notEqualToDifferentType() {
                assertThat(record).isNotEqualTo("PR001");
            }

            @Test
            @DisplayName("equal records have the same hashCode")
            void equalObjects_sameHashCode() {
                PayrollRecord twin = PayrollRecord.builder()
                        .id("PR001").employeeId("E999")
                        .grossSalary(new BigDecimal("1"))
                        .taxAmount(BigDecimal.ZERO)
                        .payrollMonth(APRIL_2026).processedTimestamp(PROCESSED_TS)
                        .build();

                assertThat(record.hashCode()).isEqualTo(twin.hashCode());
            }

            @Test
            @DisplayName("hashCode is stable across multiple calls")
            void hashCode_stable() {
                int first = record.hashCode();
                assertThat(record.hashCode()).isEqualTo(first);
            }
        }

        // ------------------------------------------------------------------
        // ToString
        // ------------------------------------------------------------------

        @Nested
        @DisplayName("toString")
        class ToStringTests {

            @Test
            @DisplayName("contains the record id")
            void containsId() {
                assertThat(record.toString()).contains("PR001");
            }

            @Test
            @DisplayName("contains the employeeId")
            void containsEmployeeId() {
                assertThat(record.toString()).contains("E001");
            }

            @Test
            @DisplayName("contains the grossSalary")
            void containsGrossSalary() {
                assertThat(record.toString()).contains("100000");
            }

            @Test
            @DisplayName("contains the taxAmount")
            void containsTaxAmount() {
                assertThat(record.toString()).contains("20000");
            }
        }
    }
}
