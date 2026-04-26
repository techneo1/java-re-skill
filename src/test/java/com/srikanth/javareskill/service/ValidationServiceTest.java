package com.srikanth.javareskill.service;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.EmployeeId;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.exception.DepartmentNotFoundException;
import com.srikanth.javareskill.exception.DuplicateEmailException;
import com.srikanth.javareskill.exception.InvalidSalaryException;
import com.srikanth.javareskill.repository.inmemory.InMemoryDepartmentRepository;
import com.srikanth.javareskill.repository.inmemory.InMemoryEmployeeRepository;
import com.srikanth.javareskill.service.impl.ValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ValidationServiceImpl}.
 *
 * <p>Tests are grouped into three nested classes, one per validation rule:</p>
 * <ul>
 *   <li>{@link EmailUniquenessTest}  – e-mail uniqueness</li>
 *   <li>{@link SalaryRangeTest}      – salary range enforcement</li>
 *   <li>{@link DepartmentExistsTest} – department existence</li>
 * </ul>
 */
@DisplayName("ValidationService")
class ValidationServiceTest {

    private InMemoryEmployeeRepository   employeeRepo;
    private InMemoryDepartmentRepository departmentRepo;
    private ValidationService            validator;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Employee employee(String id, String email) {
        return Employee.builder()
                .id(id)
                .name("Name-" + id)
                .email(email)
                .departmentId("D1")
                .role(Role.ENGINEER)
                .salary(new BigDecimal("60000"))
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2024, 1, 1))
                .build();
    }

    private static Department department(String id) {
        return Department.builder()
                .id(id)
                .name("Dept-" + id)
                .location("HQ")
                .build();
    }

    @BeforeEach
    void setUp() {
        employeeRepo   = new InMemoryEmployeeRepository();
        departmentRepo = new InMemoryDepartmentRepository();
        validator      = new ValidationServiceImpl(employeeRepo, departmentRepo);
    }

    // =========================================================================
    // 1. E-mail uniqueness
    // =========================================================================

    @Nested
    @DisplayName("validateEmailUniqueness")
    class EmailUniquenessTest {

        @Test
        @DisplayName("passes when no employees exist")
        void noEmployees_passes() {
            assertThatNoException()
                    .isThrownBy(() -> validator.validateEmailUniqueness("alice@example.com"));
        }

        @Test
        @DisplayName("passes when email belongs to a different employee")
        void differentEmployee_passes() {
            employeeRepo.save(employee("E001", "bob@example.com"));
            assertThatNoException()
                    .isThrownBy(() -> validator.validateEmailUniqueness("alice@example.com"));
        }

        @Test
        @DisplayName("throws DuplicateEmailException when email already exists")
        void duplicateEmail_throwsDuplicateEmailException() {
            employeeRepo.save(employee("E001", "alice@example.com"));

            assertThatThrownBy(() -> validator.validateEmailUniqueness("alice@example.com"))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasMessageContaining("alice@example.com")
                    .extracting(e -> ((DuplicateEmailException) e).getExistingEmployeeId())
                    .isEqualTo("E001");
        }

        @Test
        @DisplayName("email check is case-insensitive")
        void duplicateEmail_caseInsensitive_throwsDuplicateEmailException() {
            employeeRepo.save(employee("E001", "Alice@Example.COM"));

            assertThatThrownBy(() -> validator.validateEmailUniqueness("alice@example.com"))
                    .isInstanceOf(DuplicateEmailException.class);
        }

        @Test
        @DisplayName("passes when excluded employee owns the email (update scenario)")
        void excludedEmployee_passes() {
            employeeRepo.save(employee("E001", "alice@example.com"));

            // Updating E001 with its own email — should be allowed
            assertThatNoException()
                    .isThrownBy(() -> validator.validateEmailUniqueness("alice@example.com", "E001"));
        }

        @Test
        @DisplayName("throws DuplicateEmailException when a different employee owns the email (update scenario)")
        void excludedEmployee_otherOwner_throwsDuplicateEmailException() {
            employeeRepo.save(employee("E001", "alice@example.com"));
            employeeRepo.save(employee("E002", "bob@example.com"));

            // Updating E002 but trying to take E001's email
            assertThatThrownBy(() -> validator.validateEmailUniqueness("alice@example.com", "E002"))
                    .isInstanceOf(DuplicateEmailException.class)
                    .extracting(e -> ((DuplicateEmailException) e).getExistingEmployeeId())
                    .isEqualTo("E001");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for null email")
        void nullEmail_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> validator.validateEmailUniqueness(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for blank email")
        void blankEmail_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> validator.validateEmailUniqueness("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // 2. Salary range
    // =========================================================================

    @Nested
    @DisplayName("validateSalaryRange")
    class SalaryRangeTest {

        private static final BigDecimal MIN = new BigDecimal("30000");
        private static final BigDecimal MAX = new BigDecimal("200000");

        @Test
        @DisplayName("passes for salary exactly at the minimum bound")
        void salaryAtMin_passes() {
            assertThatNoException()
                    .isThrownBy(() -> validator.validateSalaryRange(MIN, MIN, MAX));
        }

        @Test
        @DisplayName("passes for salary exactly at the maximum bound")
        void salaryAtMax_passes() {
            assertThatNoException()
                    .isThrownBy(() -> validator.validateSalaryRange(MAX, MIN, MAX));
        }

        @Test
        @DisplayName("passes for salary within range")
        void salaryWithinRange_passes() {
            assertThatNoException()
                    .isThrownBy(() -> validator.validateSalaryRange(
                            new BigDecimal("75000"), MIN, MAX));
        }

        @Test
        @DisplayName("throws InvalidSalaryException for negative salary")
        void negativeSalary_throwsInvalidSalaryException() {
            assertThatThrownBy(() -> validator.validateSalaryRange(
                    new BigDecimal("-1"), MIN, MAX))
                    .isInstanceOf(InvalidSalaryException.class)
                    .extracting(e -> ((InvalidSalaryException) e).getInvalidValue())
                    .isEqualTo(new BigDecimal("-1"));
        }

        @Test
        @DisplayName("throws InvalidSalaryException for salary below minimum")
        void salaryBelowMin_throwsInvalidSalaryException() {
            BigDecimal tooLow = new BigDecimal("10000");
            assertThatThrownBy(() -> validator.validateSalaryRange(tooLow, MIN, MAX))
                    .isInstanceOf(InvalidSalaryException.class)
                    .hasMessageContaining("10000");
        }

        @Test
        @DisplayName("throws InvalidSalaryException for salary above maximum")
        void salaryAboveMax_throwsInvalidSalaryException() {
            BigDecimal tooHigh = new BigDecimal("500000");
            assertThatThrownBy(() -> validator.validateSalaryRange(tooHigh, MIN, MAX))
                    .isInstanceOf(InvalidSalaryException.class)
                    .hasMessageContaining("500000");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when min > max")
        void minGreaterThanMax_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> validator.validateSalaryRange(
                    new BigDecimal("50000"), MAX, MIN))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws NullPointerException for null salary")
        void nullSalary_throwsNullPointerException() {
            assertThatThrownBy(() -> validator.validateSalaryRange(null, MIN, MAX))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("passes for salary equal to min and max (single-value range)")
        void singleValueRange_passes() {
            BigDecimal exact = new BigDecimal("50000");
            assertThatNoException()
                    .isThrownBy(() -> validator.validateSalaryRange(exact, exact, exact));
        }
    }

    // =========================================================================
    // 3. Department existence
    // =========================================================================

    @Nested
    @DisplayName("validateDepartmentExists")
    class DepartmentExistsTest {

        @Test
        @DisplayName("passes when department exists")
        void departmentExists_passes() {
            departmentRepo.save(department("D1"));
            assertThatNoException()
                    .isThrownBy(() -> validator.validateDepartmentExists("D1"));
        }

        @Test
        @DisplayName("throws DepartmentNotFoundException when department is absent")
        void departmentAbsent_throwsDepartmentNotFoundException() {
            assertThatThrownBy(() -> validator.validateDepartmentExists("GHOST-99"))
                    .isInstanceOf(DepartmentNotFoundException.class)
                    .hasMessageContaining("GHOST-99");
        }

        @Test
        @DisplayName("throws DepartmentNotFoundException after department is deleted")
        void departmentDeleted_throwsDepartmentNotFoundException() {
            departmentRepo.save(department("D1"));
            departmentRepo.deleteById("D1");

            assertThatThrownBy(() -> validator.validateDepartmentExists("D1"))
                    .isInstanceOf(DepartmentNotFoundException.class);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for null departmentId")
        void nullDepartmentId_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> validator.validateDepartmentExists(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for blank departmentId")
        void blankDepartmentId_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> validator.validateDepartmentExists("  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("passes for multiple departments when the queried one exists")
        void multipleDepartments_correctOneFound_passes() {
            departmentRepo.save(department("D1"));
            departmentRepo.save(department("D2"));
            departmentRepo.save(department("D3"));

            assertThatNoException()
                    .isThrownBy(() -> validator.validateDepartmentExists("D2"));
        }
    }

    // =========================================================================
    // Constructor guard
    // =========================================================================

    @Nested
    @DisplayName("constructor")
    class ConstructorTest {

        @Test
        @DisplayName("throws NullPointerException when employeeRepository is null")
        void nullEmployeeRepository_throwsNullPointerException() {
            assertThatThrownBy(() -> new ValidationServiceImpl(null, departmentRepo))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws NullPointerException when departmentRepository is null")
        void nullDepartmentRepository_throwsNullPointerException() {
            assertThatThrownBy(() -> new ValidationServiceImpl(employeeRepo, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}

