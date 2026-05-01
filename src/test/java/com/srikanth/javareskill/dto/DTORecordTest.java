package com.srikanth.javareskill.dto;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.PayrollRecord;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the three DTO records: {@link EmployeeDTO}, {@link DepartmentDTO},
 * and {@link PayrollSummaryDTO}.
 *
 * <h2>What is verified per record</h2>
 * <ul>
 *   <li><b>Construction</b>         – all components accessible via record accessors</li>
 *   <li><b>Null validation</b>      – compact constructor rejects null components</li>
 *   <li><b>fromEntity() factory</b> – faithful projection from domain entity</li>
 *   <li><b>equals / hashCode</b>    – records with same components are equal</li>
 *   <li><b>toString</b>             – contains key values</li>
 * </ul>
 */
@DisplayName("DTO Records")
class DTORecordTest {

    // =========================================================================
    // EmployeeDTO
    // =========================================================================

    @Nested
    @DisplayName("EmployeeDTO")
    class EmployeeDTOTests {

        private final EmployeeDTO dto = new EmployeeDTO(
                "E001", "Alice Smith", "alice@example.com", "D001",
                Role.ENGINEER, new BigDecimal("75000"),
                EmployeeStatus.ACTIVE, LocalDate.of(2022, 6, 1));

        // --- Construction & accessors ---

        @Nested
        @DisplayName("Construction")
        class Construction {

            @Test
            @DisplayName("all components are accessible via record accessors")
            void allAccessors() {
                assertThat(dto.id())           .isEqualTo("E001");
                assertThat(dto.name())         .isEqualTo("Alice Smith");
                assertThat(dto.email())        .isEqualTo("alice@example.com");
                assertThat(dto.departmentId()) .isEqualTo("D001");
                assertThat(dto.role())         .isEqualTo(Role.ENGINEER);
                assertThat(dto.salary())       .isEqualByComparingTo("75000");
                assertThat(dto.status())       .isEqualTo(EmployeeStatus.ACTIVE);
                assertThat(dto.joiningDate())  .isEqualTo(LocalDate.of(2022, 6, 1));
            }
        }

        // --- Null validation ---

        @Nested
        @DisplayName("Null validation")
        class NullValidation {

            @Test
            @DisplayName("null id throws NullPointerException")
            void nullId() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new EmployeeDTO(
                                null, "X", "x@x.com", "D1", Role.HR,
                                BigDecimal.TEN, EmployeeStatus.ACTIVE, LocalDate.now()))
                        .withMessageContaining("id");
            }

            @Test
            @DisplayName("null name throws NullPointerException")
            void nullName() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new EmployeeDTO(
                                "E1", null, "x@x.com", "D1", Role.HR,
                                BigDecimal.TEN, EmployeeStatus.ACTIVE, LocalDate.now()))
                        .withMessageContaining("name");
            }

            @Test
            @DisplayName("null email throws NullPointerException")
            void nullEmail() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new EmployeeDTO(
                                "E1", "X", null, "D1", Role.HR,
                                BigDecimal.TEN, EmployeeStatus.ACTIVE, LocalDate.now()))
                        .withMessageContaining("email");
            }

            @Test
            @DisplayName("null role throws NullPointerException")
            void nullRole() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new EmployeeDTO(
                                "E1", "X", "x@x.com", "D1", null,
                                BigDecimal.TEN, EmployeeStatus.ACTIVE, LocalDate.now()))
                        .withMessageContaining("role");
            }

            @Test
            @DisplayName("null salary throws NullPointerException")
            void nullSalary() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new EmployeeDTO(
                                "E1", "X", "x@x.com", "D1", Role.HR,
                                null, EmployeeStatus.ACTIVE, LocalDate.now()))
                        .withMessageContaining("salary");
            }
        }

        // --- fromEntity() ---

        @Nested
        @DisplayName("fromEntity()")
        class FromEntity {

            @Test
            @DisplayName("maps all fields from domain Employee to DTO")
            void mapsAllFields() {
                Employee entity = Employee.builder()
                        .id("E001").name("Alice Smith").email("alice@example.com")
                        .departmentId("D001").role(Role.ENGINEER)
                        .salary(new BigDecimal("75000"))
                        .status(EmployeeStatus.ACTIVE)
                        .joiningDate(LocalDate.of(2022, 6, 1))
                        .build();

                EmployeeDTO result = EmployeeDTO.fromEntity(entity);

                assertThat(result).isEqualTo(dto);
            }

            @Test
            @DisplayName("null entity throws NullPointerException")
            void nullEntity() {
                assertThatNullPointerException()
                        .isThrownBy(() -> EmployeeDTO.fromEntity(null));
            }
        }

        // --- equals / hashCode / toString ---

        @Nested
        @DisplayName("equals, hashCode, toString")
        class EqualsHashCodeToString {

            @Test
            @DisplayName("records with same components are equal")
            void sameComponents_equal() {
                EmployeeDTO twin = new EmployeeDTO(
                        "E001", "Alice Smith", "alice@example.com", "D001",
                        Role.ENGINEER, new BigDecimal("75000"),
                        EmployeeStatus.ACTIVE, LocalDate.of(2022, 6, 1));

                assertThat(dto).isEqualTo(twin);
                assertThat(dto.hashCode()).isEqualTo(twin.hashCode());
            }

            @Test
            @DisplayName("records with different components are not equal")
            void differentComponents_notEqual() {
                EmployeeDTO other = new EmployeeDTO(
                        "E999", "Bob", "bob@example.com", "D2",
                        Role.MANAGER, new BigDecimal("90000"),
                        EmployeeStatus.INACTIVE, LocalDate.of(2023, 1, 1));

                assertThat(dto).isNotEqualTo(other);
            }

            @Test
            @DisplayName("toString contains key values")
            void toStringContainsValues() {
                String s = dto.toString();
                assertThat(s).contains("E001", "Alice Smith", "alice@example.com", "ENGINEER");
            }
        }
    }

    // =========================================================================
    // DepartmentDTO
    // =========================================================================

    @Nested
    @DisplayName("DepartmentDTO")
    class DepartmentDTOTests {

        private final DepartmentDTO dto = new DepartmentDTO("D001", "Engineering", "New York");

        @Nested
        @DisplayName("Construction")
        class Construction {

            @Test
            @DisplayName("all components are accessible")
            void allAccessors() {
                assertThat(dto.id())       .isEqualTo("D001");
                assertThat(dto.name())     .isEqualTo("Engineering");
                assertThat(dto.location()) .isEqualTo("New York");
            }
        }

        @Nested
        @DisplayName("Null validation")
        class NullValidation {

            @Test
            @DisplayName("null id throws NullPointerException")
            void nullId() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new DepartmentDTO(null, "Eng", "NY"))
                        .withMessageContaining("id");
            }

            @Test
            @DisplayName("null name throws NullPointerException")
            void nullName() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new DepartmentDTO("D1", null, "NY"))
                        .withMessageContaining("name");
            }

            @Test
            @DisplayName("null location throws NullPointerException")
            void nullLocation() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new DepartmentDTO("D1", "Eng", null))
                        .withMessageContaining("location");
            }
        }

        @Nested
        @DisplayName("fromEntity()")
        class FromEntity {

            @Test
            @DisplayName("maps all fields from domain Department to DTO")
            void mapsAllFields() {
                Department entity = Department.builder()
                        .id("D001").name("Engineering").location("New York").build();

                DepartmentDTO result = DepartmentDTO.fromEntity(entity);

                assertThat(result).isEqualTo(dto);
            }

            @Test
            @DisplayName("null entity throws NullPointerException")
            void nullEntity() {
                assertThatNullPointerException()
                        .isThrownBy(() -> DepartmentDTO.fromEntity(null));
            }
        }

        @Nested
        @DisplayName("equals, hashCode, toString")
        class EqualsHashCodeToString {

            @Test
            @DisplayName("records with same components are equal")
            void sameComponents_equal() {
                DepartmentDTO twin = new DepartmentDTO("D001", "Engineering", "New York");

                assertThat(dto).isEqualTo(twin);
                assertThat(dto.hashCode()).isEqualTo(twin.hashCode());
            }

            @Test
            @DisplayName("records with different components are not equal")
            void differentComponents_notEqual() {
                assertThat(dto).isNotEqualTo(new DepartmentDTO("D999", "HR", "Boston"));
            }

            @Test
            @DisplayName("toString contains key values")
            void toStringContainsValues() {
                assertThat(dto.toString()).contains("D001", "Engineering", "New York");
            }
        }
    }

    // =========================================================================
    // PayrollSummaryDTO
    // =========================================================================

    @Nested
    @DisplayName("PayrollSummaryDTO")
    class PayrollSummaryDTOTests {

        private static final LocalDate APRIL_2026       = LocalDate.of(2026, 4, 1);
        private static final LocalDateTime PROCESSED_TS = LocalDateTime.of(2026, 4, 30, 12, 0);

        private final PayrollSummaryDTO dto = new PayrollSummaryDTO(
                "PR001", "E001",
                new BigDecimal("100000"), new BigDecimal("20000"), new BigDecimal("80000"),
                APRIL_2026, PROCESSED_TS);

        @Nested
        @DisplayName("Construction")
        class Construction {

            @Test
            @DisplayName("all components are accessible")
            void allAccessors() {
                assertThat(dto.id())                 .isEqualTo("PR001");
                assertThat(dto.employeeId())         .isEqualTo("E001");
                assertThat(dto.grossSalary())        .isEqualByComparingTo("100000");
                assertThat(dto.taxAmount())          .isEqualByComparingTo("20000");
                assertThat(dto.netSalary())          .isEqualByComparingTo("80000");
                assertThat(dto.payrollMonth())       .isEqualTo(APRIL_2026);
                assertThat(dto.processedTimestamp())  .isEqualTo(PROCESSED_TS);
            }

            @Test
            @DisplayName("invariant: netSalary must equal grossSalary − taxAmount")
            void invariantViolation_throwsIllegalArgumentException() {
                assertThatIllegalArgumentException()
                        .isThrownBy(() -> new PayrollSummaryDTO(
                                "PR001", "E001",
                                new BigDecimal("100000"),
                                new BigDecimal("20000"),
                                new BigDecimal("99999"),   // wrong net
                                APRIL_2026, PROCESSED_TS))
                        .withMessageContaining("netSalary must equal");
            }

            @Test
            @DisplayName("zero tax → netSalary equals grossSalary")
            void zeroTax_netEqualsGross() {
                PayrollSummaryDTO noTax = new PayrollSummaryDTO(
                        "PR002", "E001",
                        new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"),
                        APRIL_2026, PROCESSED_TS);

                assertThat(noTax.netSalary()).isEqualByComparingTo("50000");
            }
        }

        @Nested
        @DisplayName("Null validation")
        class NullValidation {

            @Test
            @DisplayName("null id throws NullPointerException")
            void nullId() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new PayrollSummaryDTO(
                                null, "E001",
                                new BigDecimal("100000"), new BigDecimal("20000"),
                                new BigDecimal("80000"), APRIL_2026, PROCESSED_TS))
                        .withMessageContaining("id");
            }

            @Test
            @DisplayName("null employeeId throws NullPointerException")
            void nullEmployeeId() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new PayrollSummaryDTO(
                                "PR001", null,
                                new BigDecimal("100000"), new BigDecimal("20000"),
                                new BigDecimal("80000"), APRIL_2026, PROCESSED_TS))
                        .withMessageContaining("employeeId");
            }

            @Test
            @DisplayName("null grossSalary throws NullPointerException")
            void nullGrossSalary() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new PayrollSummaryDTO(
                                "PR001", "E001",
                                null, new BigDecimal("20000"),
                                new BigDecimal("80000"), APRIL_2026, PROCESSED_TS))
                        .withMessageContaining("grossSalary");
            }

            @Test
            @DisplayName("null netSalary throws NullPointerException")
            void nullNetSalary() {
                assertThatNullPointerException()
                        .isThrownBy(() -> new PayrollSummaryDTO(
                                "PR001", "E001",
                                new BigDecimal("100000"), new BigDecimal("20000"),
                                null, APRIL_2026, PROCESSED_TS))
                        .withMessageContaining("netSalary");
            }
        }

        @Nested
        @DisplayName("fromEntity()")
        class FromEntity {

            @Test
            @DisplayName("maps all fields from domain PayrollRecord to DTO")
            void mapsAllFields() {
                PayrollRecord entity = PayrollRecord.builder()
                        .id("PR001").employeeId("E001")
                        .grossSalary(new BigDecimal("100000"))
                        .taxAmount(new BigDecimal("20000"))
                        .payrollMonth(APRIL_2026)
                        .processedTimestamp(PROCESSED_TS)
                        .build();

                PayrollSummaryDTO result = PayrollSummaryDTO.fromEntity(entity);

                assertThat(result).isEqualTo(dto);
            }

            @Test
            @DisplayName("null entity throws NullPointerException")
            void nullEntity() {
                assertThatNullPointerException()
                        .isThrownBy(() -> PayrollSummaryDTO.fromEntity(null));
            }
        }

        @Nested
        @DisplayName("equals, hashCode, toString")
        class EqualsHashCodeToString {

            @Test
            @DisplayName("records with same components are equal")
            void sameComponents_equal() {
                PayrollSummaryDTO twin = new PayrollSummaryDTO(
                        "PR001", "E001",
                        new BigDecimal("100000"), new BigDecimal("20000"),
                        new BigDecimal("80000"), APRIL_2026, PROCESSED_TS);

                assertThat(dto).isEqualTo(twin);
                assertThat(dto.hashCode()).isEqualTo(twin.hashCode());
            }

            @Test
            @DisplayName("records with different components are not equal")
            void differentComponents_notEqual() {
                PayrollSummaryDTO other = new PayrollSummaryDTO(
                        "PR999", "E002",
                        new BigDecimal("50000"), new BigDecimal("5000"),
                        new BigDecimal("45000"), APRIL_2026, PROCESSED_TS);

                assertThat(dto).isNotEqualTo(other);
            }

            @Test
            @DisplayName("toString contains key values")
            void toStringContainsValues() {
                assertThat(dto.toString()).contains("PR001", "E001", "100000", "20000", "80000");
            }
        }
    }
}

