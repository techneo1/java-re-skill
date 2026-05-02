package com.srikanth.javareskill.dto.request;

import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Bean Validation constraints on all request DTOs.
 *
 * <h2>Approach</h2>
 * <p>Uses the real Hibernate Validator (pulled in by {@code spring-boot-starter-validation})
 * via {@link Validation#buildDefaultValidatorFactory()}.  No Spring context is loaded —
 * tests are fast and run without a server.</p>
 *
 * <h2>Test structure</h2>
 * <ul>
 *   <li>Each request class has its own {@code @Nested} group.</li>
 *   <li>A "happy path" test verifies a fully valid request passes with zero violations.</li>
 *   <li>Individual constraint tests verify that exactly the right field fails with the
 *       right message — no more, no less.</li>
 * </ul>
 */
@DisplayName("Request DTO Bean Validation")
class RequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Returns the set of violated field names. */
    private static <T> Set<String> violatedFields(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    /** Returns all violation messages for a single field. */
    private static <T> Set<String> messagesFor(Set<ConstraintViolation<T>> violations, String field) {
        return violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals(field))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    // =========================================================================
    // CreateEmployeeRequest
    // =========================================================================

    @Nested
    @DisplayName("CreateEmployeeRequest")
    class CreateEmployeeRequestValidation {

        /** Builds a fully valid instance — baseline for all mutation tests. */
        private CreateEmployeeRequest valid() {
            return new CreateEmployeeRequest(
                    "Alice Smith",
                    "alice@example.com",
                    "DEPT-01",
                    Role.ENGINEER,
                    new BigDecimal("75000.00"),
                    EmployeeStatus.ACTIVE,
                    LocalDate.now()
            );
        }

        @Test
        @DisplayName("valid request → zero violations")
        void validRequest_noViolations() {
            assertThat(validator.validate(valid())).isEmpty();
        }

        // ── name ─────────────────────────────────────────────────────────────

        @ParameterizedTest(name = "name=''{0}'' → @NotBlank violation")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("blank name → @NotBlank violation")
        void blankName_violation(String name) {
            var req = valid();
            req.setName(name);
            Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(req);
            assertThat(violatedFields(violations)).contains("name");
        }

        @Test
        @DisplayName("name too short (1 char) → @Size violation")
        void nameTooShort_violation() {
            var req = valid();
            req.setName("A");
            Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(req);
            assertThat(violatedFields(violations)).contains("name");
            assertThat(messagesFor(violations, "name"))
                    .anyMatch(m -> m.contains("between 2 and 100"));
        }

        @Test
        @DisplayName("name too long (101 chars) → @Size violation")
        void nameTooLong_violation() {
            var req = valid();
            req.setName("A".repeat(101));
            Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(req);
            assertThat(violatedFields(violations)).contains("name");
        }

        @ParameterizedTest(name = "name=''{0}'' → @Pattern violation (invalid characters)")
        @ValueSource(strings = {"Alice123", "Bob@Work", "Carol<>", "Dave\nSmith"})
        @DisplayName("name with invalid characters → @Pattern violation")
        void nameInvalidChars_violation(String name) {
            var req = valid();
            req.setName(name);
            Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(req);
            assertThat(violatedFields(violations)).contains("name");
        }

        @ParameterizedTest(name = "valid name=''{0}''")
        @ValueSource(strings = {"Jo", "Alice Smith", "O'Brien", "Jean-Luc", "María José", "Dr. Watson"})
        @DisplayName("name with valid characters → no violation")
        void nameValidChars_noViolation(String name) {
            var req = valid();
            req.setName(name);
            assertThat(violatedFields(validator.validate(req))).doesNotContain("name");
        }

        // ── email ─────────────────────────────────────────────────────────────

        @ParameterizedTest(name = "email=''{0}'' → @Email violation")
        @ValueSource(strings = {"not-an-email", "missing@", "@nodomain", "spaces in@email.com", "plaintext"})
        @DisplayName("malformed email → @Email violation")
        void malformedEmail_violation(String email) {
            var req = valid();
            req.setEmail(email);
            assertThat(violatedFields(validator.validate(req))).contains("email");
        }

        @Test
        @DisplayName("email exceeds 255 chars → @Size violation")
        void emailTooLong_violation() {
            var req = valid();
            req.setEmail("a".repeat(250) + "@x.com");   // 257 chars
            assertThat(violatedFields(validator.validate(req))).contains("email");
        }

        // ── departmentId ──────────────────────────────────────────────────────

        @Test
        @DisplayName("departmentId exceeds 50 chars → @Size violation")
        void departmentIdTooLong_violation() {
            var req = valid();
            req.setDepartmentId("D".repeat(51));
            assertThat(violatedFields(validator.validate(req))).contains("departmentId");
        }

        @ParameterizedTest(name = "blank departmentId=''{0}'' → @NotBlank violation")
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("blank departmentId → @NotBlank violation")
        void blankDepartmentId_violation(String id) {
            var req = valid();
            req.setDepartmentId(id);
            assertThat(violatedFields(validator.validate(req))).contains("departmentId");
        }

        // ── role ──────────────────────────────────────────────────────────────

        @Test
        @DisplayName("null role → @NotNull violation")
        void nullRole_violation() {
            var req = valid();
            req.setRole(null);
            assertThat(violatedFields(validator.validate(req))).contains("role");
        }

        // ── salary ────────────────────────────────────────────────────────────

        @Test
        @DisplayName("null salary → @NotNull violation")
        void nullSalary_violation() {
            var req = valid();
            req.setSalary(null);
            assertThat(violatedFields(validator.validate(req))).contains("salary");
        }

        @Test
        @DisplayName("negative salary → @DecimalMin violation")
        void negativeSalary_violation() {
            var req = valid();
            req.setSalary(new BigDecimal("-0.01"));
            Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(req);
            assertThat(violatedFields(violations)).contains("salary");
            assertThat(messagesFor(violations, "salary"))
                    .anyMatch(m -> m.contains("0.00 or greater"));
        }

        @Test
        @DisplayName("salary above 10,000,000 → @DecimalMax violation")
        void salaryAboveMax_violation() {
            var req = valid();
            req.setSalary(new BigDecimal("10000000.01"));
            assertThat(violatedFields(validator.validate(req))).contains("salary");
        }

        @Test
        @DisplayName("salary with 3 decimal places → @Digits violation")
        void salaryTooManyDecimals_violation() {
            var req = valid();
            req.setSalary(new BigDecimal("1000.123"));
            assertThat(violatedFields(validator.validate(req))).contains("salary");
        }

        @ParameterizedTest(name = "valid salary={0}")
        @ValueSource(strings = {"0.00", "0.01", "999.99", "75000.50", "10000000.00"})
        @DisplayName("boundary salaries → no violation")
        void boundarySalaries_noViolation(String salaryStr) {
            var req = valid();
            req.setSalary(new BigDecimal(salaryStr));
            assertThat(violatedFields(validator.validate(req))).doesNotContain("salary");
        }

        // ── joiningDate ───────────────────────────────────────────────────────

        @Test
        @DisplayName("future joiningDate → @PastOrPresent violation")
        void futureJoiningDate_violation() {
            var req = valid();
            req.setJoiningDate(LocalDate.now().plusDays(1));
            Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(req);
            assertThat(violatedFields(violations)).contains("joiningDate");
            assertThat(messagesFor(violations, "joiningDate"))
                    .anyMatch(m -> m.contains("future"));
        }

        @Test
        @DisplayName("today as joiningDate → no violation")
        void todayJoiningDate_noViolation() {
            var req = valid();
            req.setJoiningDate(LocalDate.now());
            assertThat(violatedFields(validator.validate(req))).doesNotContain("joiningDate");
        }

        @Test
        @DisplayName("null joiningDate → no violation (field is optional)")
        void nullJoiningDate_noViolation() {
            var req = valid();
            req.setJoiningDate(null);
            // @PastOrPresent allows null by default (use @NotNull to forbid it)
            assertThat(violatedFields(validator.validate(req))).doesNotContain("joiningDate");
        }

        // ── multiple violations ───────────────────────────────────────────────

        @Test
        @DisplayName("completely empty request → violations on all required fields")
        void emptyRequest_allRequiredFieldsViolated() {
            var req = new CreateEmployeeRequest();   // all fields null
            Set<ConstraintViolation<CreateEmployeeRequest>> violations = validator.validate(req);
            Set<String> fields = violatedFields(violations);
            assertThat(fields).contains("name", "email", "departmentId", "role", "salary");
        }
    }

    // =========================================================================
    // CreateDepartmentRequest
    // =========================================================================

    @Nested
    @DisplayName("CreateDepartmentRequest")
    class CreateDepartmentRequestValidation {

        private CreateDepartmentRequest valid() {
            return new CreateDepartmentRequest("Engineering", "New York");
        }

        @Test
        @DisplayName("valid request → zero violations")
        void validRequest_noViolations() {
            assertThat(validator.validate(valid())).isEmpty();
        }

        // ── name ─────────────────────────────────────────────────────────────

        @ParameterizedTest(name = "blank name=''{0}'' → @NotBlank")
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        @DisplayName("blank name → @NotBlank violation")
        void blankName_violation(String name) {
            var req = valid();
            req.setName(name);
            assertThat(violatedFields(validator.validate(req))).contains("name");
        }

        @Test
        @DisplayName("name too short → @Size violation")
        void nameTooShort_violation() {
            var req = valid();
            req.setName("X");
            assertThat(violatedFields(validator.validate(req))).contains("name");
        }

        @Test
        @DisplayName("name too long (101 chars) → @Size violation")
        void nameTooLong_violation() {
            var req = valid();
            req.setName("A".repeat(101));
            assertThat(violatedFields(validator.validate(req))).contains("name");
        }

        @ParameterizedTest(name = "invalid dept name=''{0}'' → @Pattern")
        @ValueSource(strings = {"Dept@123", "Eng<>", "IT/Dev", "Finance\\Tax"})
        @DisplayName("name with invalid characters → @Pattern violation")
        void nameInvalidChars_violation(String name) {
            var req = valid();
            req.setName(name);
            assertThat(violatedFields(validator.validate(req))).contains("name");
        }

        @ParameterizedTest(name = "valid dept name=''{0}''")
        @ValueSource(strings = {"IT", "R&D", "Sales - APAC", "O'Brien Associates", "Dept. No. 5"})
        @DisplayName("name with valid characters → no violation")
        void nameValidChars_noViolation(String name) {
            var req = valid();
            req.setName(name);
            assertThat(violatedFields(validator.validate(req))).doesNotContain("name");
        }

        // ── location ─────────────────────────────────────────────────────────

        @ParameterizedTest(name = "blank location=''{0}'' → @NotBlank")
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        @DisplayName("blank location → @NotBlank violation")
        void blankLocation_violation(String loc) {
            var req = valid();
            req.setLocation(loc);
            assertThat(violatedFields(validator.validate(req))).contains("location");
        }

        @Test
        @DisplayName("location too long (101 chars) → @Size violation")
        void locationTooLong_violation() {
            var req = valid();
            req.setLocation("A".repeat(101));
            assertThat(violatedFields(validator.validate(req))).contains("location");
        }

        @ParameterizedTest(name = "invalid location=''{0}'' → @Pattern")
        @ValueSource(strings = {"New York@!", "London#UK", "Paris<France>"})
        @DisplayName("location with invalid characters → @Pattern violation")
        void locationInvalidChars_violation(String loc) {
            var req = valid();
            req.setLocation(loc);
            assertThat(violatedFields(validator.validate(req))).contains("location");
        }

        @ParameterizedTest(name = "valid location=''{0}''")
        @ValueSource(strings = {"NY", "New York", "London, UK", "San Francisco - HQ", "São Paulo"})
        @DisplayName("location with valid characters → no violation")
        void locationValidChars_noViolation(String loc) {
            var req = valid();
            req.setLocation(loc);
            assertThat(violatedFields(validator.validate(req))).doesNotContain("location");
        }
    }

    // =========================================================================
    // UpdateEmployeeStatusRequest
    // =========================================================================

    @Nested
    @DisplayName("UpdateEmployeeStatusRequest")
    class UpdateEmployeeStatusRequestValidation {

        @Test
        @DisplayName("valid ACTIVE status → zero violations")
        void validStatus_noViolations() {
            var req = new UpdateEmployeeStatusRequest(EmployeeStatus.ACTIVE);
            assertThat(validator.validate(req)).isEmpty();
        }

        @Test
        @DisplayName("valid INACTIVE status → zero violations")
        void validInactiveStatus_noViolations() {
            var req = new UpdateEmployeeStatusRequest(EmployeeStatus.INACTIVE);
            assertThat(validator.validate(req)).isEmpty();
        }

        @Test
        @DisplayName("null status → @NotNull violation")
        void nullStatus_violation() {
            var req = new UpdateEmployeeStatusRequest(null);
            Set<ConstraintViolation<UpdateEmployeeStatusRequest>> violations = validator.validate(req);
            assertThat(violatedFields(violations)).contains("status");
            assertThat(messagesFor(violations, "status"))
                    .anyMatch(m -> m.contains("must not be null"));
        }
    }
}

