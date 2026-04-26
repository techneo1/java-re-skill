package com.srikanth.javareskill.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the error-code layer:
 * {@link ErrorCode}, {@link ErrorContext}, and the {@link HrException#getErrorCode()} /
 * {@link HrException#getErrorContext()} accessors on every concrete exception.
 */
@DisplayName("Error-code layer")
class ErrorCodeLayerTest {

    // =========================================================================
    // ErrorCode enum
    // =========================================================================

    @Nested
    @DisplayName("ErrorCode")
    class ErrorCodeTest {

        @Test
        @DisplayName("every code has a non-blank machine-readable code string")
        void allCodesHaveNonBlankCodeString() {
            for (ErrorCode ec : ErrorCode.values()) {
                assertThat(ec.getCode())
                        .as("ErrorCode.%s.getCode()", ec.name())
                        .isNotBlank();
            }
        }

        @Test
        @DisplayName("every code has a non-blank description")
        void allCodesHaveDescription() {
            for (ErrorCode ec : ErrorCode.values()) {
                assertThat(ec.getDescription())
                        .as("ErrorCode.%s.getDescription()", ec.name())
                        .isNotBlank();
            }
        }

        @Test
        @DisplayName("code strings follow the HR-{CAT}-{seq} pattern")
        void codeStringsMatchPattern() {
            for (ErrorCode ec : ErrorCode.values()) {
                assertThat(ec.getCode())
                        .as("ErrorCode.%s pattern", ec.name())
                        .matches("HR-[A-Z]+-\\d+");
            }
        }

        @Test
        @DisplayName("toString returns 'code: description' format")
        void toStringFormat() {
            assertThat(ErrorCode.EMPLOYEE_NOT_FOUND.toString())
                    .isEqualTo("HR-RES-001: The requested employee does not exist");
        }

        @Test
        @DisplayName("all code strings are unique")
        void codeStringsAreUnique() {
            long distinctCount = java.util.Arrays.stream(ErrorCode.values())
                    .map(ErrorCode::getCode)
                    .distinct()
                    .count();
            assertThat(distinctCount).isEqualTo(ErrorCode.values().length);
        }

        @Test
        @DisplayName("spot-check known codes")
        void knownCodes() {
            assertThat(ErrorCode.EMPLOYEE_NOT_FOUND.getCode())   .isEqualTo("HR-RES-001");
            assertThat(ErrorCode.DEPARTMENT_NOT_FOUND.getCode()) .isEqualTo("HR-RES-002");
            assertThat(ErrorCode.DUPLICATE_EMAIL.getCode())      .isEqualTo("HR-BIZ-001");
            assertThat(ErrorCode.INVALID_SALARY.getCode())       .isEqualTo("HR-BIZ-002");
            assertThat(ErrorCode.CONFIGURATION_ERROR.getCode())  .isEqualTo("HR-CFG-001");
            assertThat(ErrorCode.INVALID_ARGUMENT.getCode())     .isEqualTo("HR-VAL-001");
            assertThat(ErrorCode.NULL_SALARY.getCode())          .isEqualTo("HR-VAL-002");
            assertThat(ErrorCode.INVALID_SALARY_RANGE.getCode()) .isEqualTo("HR-VAL-003");
        }
    }

    // =========================================================================
    // ErrorContext
    // =========================================================================

    @Nested
    @DisplayName("ErrorContext")
    class ErrorContextTest {

        @Test
        @DisplayName("simple factory stores code and message")
        void simpleFactory_storesCodeAndMessage() {
            ErrorContext ctx = ErrorContext.simple(ErrorCode.EMPLOYEE_NOT_FOUND, "not found");

            assertThat(ctx.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
            assertThat(ctx.getMessage()).isEqualTo("not found");
            assertThat(ctx.getDetails()).isEmpty();
        }

        @Test
        @DisplayName("builder attaches key/value details")
        void builder_attachesDetails() {
            ErrorContext ctx = ErrorContext.of(ErrorCode.DUPLICATE_EMAIL, "dup")
                    .with("email", "a@b.com")
                    .with("existingEmployeeId", "E001")
                    .build();

            assertThat(ctx.getDetails())
                    .containsEntry("email", "a@b.com")
                    .containsEntry("existingEmployeeId", "E001");
        }

        @Test
        @DisplayName("timestamp defaults to a non-null instant close to now")
        void timestamp_defaultsToNow() {
            Instant before = Instant.now();
            ErrorContext ctx = ErrorContext.simple(ErrorCode.INVALID_SALARY, "bad");
            Instant after = Instant.now();

            assertThat(ctx.getTimestamp()).isBetween(before, after);
        }

        @Test
        @DisplayName("builder allows overriding timestamp")
        void builder_overridesTimestamp() {
            Instant fixed = Instant.parse("2024-01-01T00:00:00Z");
            ErrorContext ctx = ErrorContext.of(ErrorCode.CONFIGURATION_ERROR, "cfg")
                    .timestamp(fixed)
                    .build();

            assertThat(ctx.getTimestamp()).isEqualTo(fixed);
        }

        @Test
        @DisplayName("details map is unmodifiable")
        void detailsMap_isUnmodifiable() {
            ErrorContext ctx = ErrorContext.of(ErrorCode.DEPARTMENT_NOT_FOUND, "msg")
                    .with("k", "v")
                    .build();

            assertThatThrownBy(() -> ctx.getDetails().put("hack", "val"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("toString contains code and message")
        void toString_containsCodeAndMessage() {
            ErrorContext ctx = ErrorContext.simple(ErrorCode.EMPLOYEE_NOT_FOUND, "missing E001");
            assertThat(ctx.toString())
                    .contains("HR-RES-001")
                    .contains("missing E001");
        }

        @Test
        @DisplayName("throws NullPointerException when errorCode is null")
        void nullErrorCode_throwsNPE() {
            assertThatThrownBy(() -> ErrorContext.of(null, "msg").build())
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws NullPointerException when message is null")
        void nullMessage_throwsNPE() {
            assertThatThrownBy(() -> ErrorContext.of(ErrorCode.EMPLOYEE_NOT_FOUND, null).build())
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("equals and hashCode are consistent")
        void equalsAndHashCode() {
            Instant ts = Instant.parse("2024-06-01T12:00:00Z");
            ErrorContext a = ErrorContext.of(ErrorCode.DUPLICATE_EMAIL, "dup")
                    .with("email", "x@y.com").timestamp(ts).build();
            ErrorContext b = ErrorContext.of(ErrorCode.DUPLICATE_EMAIL, "dup")
                    .with("email", "x@y.com").timestamp(ts).build();

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }
    }

    // =========================================================================
    // Exception — error code propagation
    // =========================================================================

    @Nested
    @DisplayName("Exception error-code propagation")
    class ExceptionPropagationTest {

        @Test
        @DisplayName("EmployeeNotFoundException carries EMPLOYEE_NOT_FOUND")
        void employeeNotFound_code() {
            EmployeeNotFoundException ex = new EmployeeNotFoundException("E999");

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
            assertThat(ex.getErrorContext().getDetails()).containsEntry("employeeId", "E999");
            assertThat(ex.getMessage()).contains("E999");
        }

        @Test
        @DisplayName("DepartmentNotFoundException carries DEPARTMENT_NOT_FOUND")
        void departmentNotFound_code() {
            DepartmentNotFoundException ex = new DepartmentNotFoundException("D999");

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
            assertThat(ex.getErrorContext().getDetails()).containsEntry("departmentId", "D999");
            assertThat(ex.getMessage()).contains("D999");
        }

        @Test
        @DisplayName("DuplicateEmailException carries DUPLICATE_EMAIL")
        void duplicateEmail_code() {
            DuplicateEmailException ex = new DuplicateEmailException("a@b.com", "E001");

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL);
            assertThat(ex.getErrorContext().getDetails())
                    .containsEntry("email", "a@b.com")
                    .containsEntry("existingEmployeeId", "E001");
        }

        @Test
        @DisplayName("InvalidSalaryException carries INVALID_SALARY")
        void invalidSalary_code() {
            BigDecimal bad = new BigDecimal("-100");
            InvalidSalaryException ex = new InvalidSalaryException(bad, "negative");

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_SALARY);
            assertThat(ex.getErrorContext().getDetails()).containsEntry("invalidValue", bad);
            assertThat(ex.getInvalidValue()).isEqualTo(bad);
        }

        @Test
        @DisplayName("ConfigurationException carries CONFIGURATION_ERROR")
        void configurationException_code() {
            ConfigurationException ex = new ConfigurationException("app.properties", "missing key");

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONFIGURATION_ERROR);
            assertThat(ex.getErrorContext().getDetails()).containsEntry("configContext", "app.properties");
        }

        @Test
        @DisplayName("error code is accessible via base HrException reference (polymorphism)")
        void polymorphicAccess() {
            HrException ex = new EmployeeNotFoundException("E001");
            assertThat(ex.getErrorCode().getCode()).isEqualTo("HR-RES-001");
        }

        @Test
        @DisplayName("with-cause constructor also propagates error code")
        void withCause_propagatesCode() {
            RuntimeException cause = new RuntimeException("db error");
            EmployeeNotFoundException ex = new EmployeeNotFoundException("E001", cause);

            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMPLOYEE_NOT_FOUND);
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }
}

