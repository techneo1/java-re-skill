package com.srikanth.javareskill.domain;

import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class DomainTest {

    // ------------------------------------------------------------------ Employee

    @Test
    void employee_buildsCorrectly() {
        Employee e = Employee.builder()
                .id("E001")
                .name("Alice Smith")
                .email("alice@example.com")
                .departmentId("D001")
                .role(Role.ENGINEER)
                .salary(new BigDecimal("75000.00"))
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2022, 6, 1))
                .build();

        assertThat(e.getId()).isEqualTo("E001");
        assertThat(e.getName()).isEqualTo("Alice Smith");
        assertThat(e.getRole()).isEqualTo(Role.ENGINEER);
        assertThat(e.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    void employee_throwsOnNullId() {
        assertThatNullPointerException().isThrownBy(() ->
                Employee.builder()
                        .name("Bob")
                        .email("bob@example.com")
                        .departmentId("D001")
                        .role(Role.ANALYST)
                        .salary(BigDecimal.TEN)
                        .status(EmployeeStatus.ACTIVE)
                        .joiningDate(LocalDate.now())
                        .build()
        ).withMessageContaining("id");
    }

    @Test
    void employee_throwsOnNegativeSalary() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                Employee.builder()
                        .id("E002")
                        .name("Carol")
                        .email("carol@example.com")
                        .departmentId("D001")
                        .role(Role.MANAGER)
                        .salary(new BigDecimal("-1"))
                        .status(EmployeeStatus.ACTIVE)
                        .joiningDate(LocalDate.now())
                        .build()
        );
    }

    @Test
    void employee_equalityBasedOnId() {
        Employee e1 = buildEmployee("E001");
        Employee e2 = buildEmployee("E001");
        Employee e3 = buildEmployee("E999");

        assertThat(e1).isEqualTo(e2);
        assertThat(e1).isNotEqualTo(e3);
    }

    // ------------------------------------------------------------------ Department

    @Test
    void department_buildsCorrectly() {
        Department d = Department.builder()
                .id("D001")
                .name("Engineering")
                .location("New York")
                .build();

        assertThat(d.getId()).isEqualTo("D001");
        assertThat(d.getName()).isEqualTo("Engineering");
        assertThat(d.getLocation()).isEqualTo("New York");
    }

    @Test
    void department_throwsOnNullName() {
        assertThatNullPointerException().isThrownBy(() ->
                Department.builder().id("D002").location("London").build()
        ).withMessageContaining("name");
    }

    @Test
    void department_equalityBasedOnId() {
        Department d1 = Department.builder().id("D001").name("Eng").location("NY").build();
        Department d2 = Department.builder().id("D001").name("Eng").location("NY").build();
        assertThat(d1).isEqualTo(d2);
        assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
    }

    // ------------------------------------------------------------------ PayrollRecord

    @Test
    void payrollRecord_buildsAndDerivesNetSalary() {
        PayrollRecord pr = PayrollRecord.builder()
                .id("PR001")
                .employeeId("E001")
                .grossSalary(new BigDecimal("100000.00"))
                .taxAmount(new BigDecimal("20000.00"))
                .payrollMonth(LocalDate.of(2024, 4, 1))
                .processedTimestamp(LocalDateTime.of(2024, 4, 30, 12, 0))
                .build();

        assertThat(pr.getNetSalary()).isEqualByComparingTo("80000.00");
    }

    @Test
    void payrollRecord_throwsWhenTaxExceedsGross() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                PayrollRecord.builder()
                        .id("PR002")
                        .employeeId("E001")
                        .grossSalary(new BigDecimal("5000.00"))
                        .taxAmount(new BigDecimal("6000.00"))
                        .payrollMonth(LocalDate.of(2024, 4, 1))
                        .processedTimestamp(LocalDateTime.now())
                        .build()
        );
    }

    @Test
    void payrollRecord_equalityBasedOnId() {
        PayrollRecord pr1 = buildPayrollRecord("PR001");
        PayrollRecord pr2 = buildPayrollRecord("PR001");
        assertThat(pr1).isEqualTo(pr2);
    }

    // ------------------------------------------------------------------ Helpers

    private Employee buildEmployee(String id) {
        return Employee.builder()
                .id(id)
                .name("Test User")
                .email("test@example.com")
                .departmentId("D001")
                .role(Role.ENGINEER)
                .salary(new BigDecimal("50000"))
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.now())
                .build();
    }

    private PayrollRecord buildPayrollRecord(String id) {
        return PayrollRecord.builder()
                .id(id)
                .employeeId("E001")
                .grossSalary(new BigDecimal("80000"))
                .taxAmount(new BigDecimal("16000"))
                .payrollMonth(LocalDate.of(2024, 1, 1))
                .processedTimestamp(LocalDateTime.now())
                .build();
    }
}

