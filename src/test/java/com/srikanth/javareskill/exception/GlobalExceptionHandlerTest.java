package com.srikanth.javareskill.exception;

import com.srikanth.javareskill.dto.response.ApiErrorResponse;
import com.srikanth.javareskill.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>No Spring context is loaded — the handler is instantiated directly and
 * a {@link MockHttpServletRequest} is passed in place of a real request.
 * This keeps the tests fast and free from application startup overhead.</p>
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest  request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/test-path");
    }

    // =========================================================================
    // 400 Bad Request
    // =========================================================================

    @Nested
    @DisplayName("400 Bad Request")
    class BadRequest {

        @Test
        @DisplayName("MethodArgumentNotValidException → 400 with fieldErrors map")
        void validationException_returns400WithFieldErrors() {
            FieldError nameError  = new FieldError("req", "name",  "must not be blank");
            FieldError emailError = new FieldError("req", "email", "must be a well-formed email address");

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(nameError, emailError));

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            ResponseEntity<ApiErrorResponse> response = handler.handleValidation(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            ApiErrorResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.status()).isEqualTo(400);
            assertThat(body.error()).isEqualTo("Bad Request");
            assertThat(body.fieldErrors()).containsKey("name");
            assertThat(body.fieldErrors()).containsKey("email");
            assertThat(body.fieldErrors().get("name")).contains("must not be blank");
            assertThat(body.path()).isEqualTo("/test-path");
        }

        @Test
        @DisplayName("MissingServletRequestParameterException → 400 with parameter name")
        void missingParam_returns400() {
            var ex = new org.springframework.web.bind.MissingServletRequestParameterException("month", "String");

            ResponseEntity<ApiErrorResponse> response = handler.handleMissingParam(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().message()).contains("month");
        }
    }

    // =========================================================================
    // 404 Not Found
    // =========================================================================

    @Nested
    @DisplayName("404 Not Found")
    class NotFound {

        @Test
        @DisplayName("EmployeeNotFoundException → 404 with employee ID in message")
        void employeeNotFound_returns404() {
            var ex = new EmployeeNotFoundException("EMP-42");

            ResponseEntity<ApiErrorResponse> response = handler.handleEmployeeNotFound(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            ApiErrorResponse body = response.getBody();
            assertThat(body.status()).isEqualTo(404);
            assertThat(body.error()).isEqualTo("Not Found");
            assertThat(body.message()).contains("EMP-42");
            assertThat(body.path()).isEqualTo("/test-path");
            assertThat(body.fieldErrors()).isEmpty();
        }

        @Test
        @DisplayName("DepartmentNotFoundException → 404 with department ID in message")
        void departmentNotFound_returns404() {
            var ex = new DepartmentNotFoundException("DEPT-99");

            ResponseEntity<ApiErrorResponse> response = handler.handleDepartmentNotFound(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().message()).contains("DEPT-99");
        }
    }

    // =========================================================================
    // 409 Conflict
    // =========================================================================

    @Nested
    @DisplayName("409 Conflict")
    class Conflict {

        @Test
        @DisplayName("DuplicateEmailException → 409 with email in message")
        void duplicateEmail_returns409() {
            var ex = new DuplicateEmailException("alice@example.com", "EMP-01");

            ResponseEntity<ApiErrorResponse> response = handler.handleDuplicateEmail(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            ApiErrorResponse body = response.getBody();
            assertThat(body.status()).isEqualTo(409);
            assertThat(body.error()).isEqualTo("Conflict");
            assertThat(body.message()).contains("alice@example.com");
        }
    }

    // =========================================================================
    // 422 Unprocessable Entity
    // =========================================================================

    @Nested
    @DisplayName("422 Unprocessable Entity")
    class UnprocessableEntity {

        @Test
        @DisplayName("InvalidSalaryException → 422 with salary value in message")
        void invalidSalary_returns422() {
            var ex = new InvalidSalaryException(new BigDecimal("-100"), "must not be negative");

            ResponseEntity<ApiErrorResponse> response = handler.handleInvalidSalary(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            ApiErrorResponse body = response.getBody();
            assertThat(body.status()).isEqualTo(422);
            assertThat(body.error()).isEqualTo("Unprocessable Entity");
            assertThat(body.message()).contains("-100");
        }
    }

    // =========================================================================
    // 500 Internal Server Error
    // =========================================================================

    @Nested
    @DisplayName("500 Internal Server Error")
    class InternalServerError {

        @Test
        @DisplayName("Unexpected Exception → 500 without leaking details")
        void unexpectedException_returns500WithoutDetails() {
            var ex = new RuntimeException("Database connection pool exhausted — internal detail");

            ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            ApiErrorResponse body = response.getBody();
            assertThat(body.status()).isEqualTo(500);
            assertThat(body.error()).isEqualTo("Internal Server Error");
            // Must NOT leak the raw exception message
            assertThat(body.message()).doesNotContain("Database connection pool exhausted");
            assertThat(body.message()).contains("unexpected internal error");
        }

        @Test
        @DisplayName("HrException (configuration) → 500 with error code reference, no stack details")
        void configurationException_returns500WithErrorCode() {
            var ex = new ConfigurationException("Missing JDBC URL");

            ResponseEntity<ApiErrorResponse> response = handler.handleHrException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().message()).contains(ErrorCode.CONFIGURATION_ERROR.getCode());
        }
    }

    // =========================================================================
    // Shared contract
    // =========================================================================

    @Nested
    @DisplayName("Shared response contract")
    class SharedContract {

        @Test
        @DisplayName("All responses carry the request path")
        void allResponsesCarryPath() {
            request.setRequestURI("/employees/E-999");
            var ex = new EmployeeNotFoundException("E-999");

            ApiErrorResponse body = handler.handleEmployeeNotFound(ex, request).getBody();

            assertThat(body.path()).isEqualTo("/employees/E-999");
        }

        @Test
        @DisplayName("All responses carry a non-null timestamp")
        void allResponsesHaveTimestamp() {
            var ex = new EmployeeNotFoundException("E-001");

            ApiErrorResponse body = handler.handleEmployeeNotFound(ex, request).getBody();

            assertThat(body.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("Non-validation responses have empty fieldErrors map")
        void nonValidationResponsesHaveEmptyFieldErrors() {
            var ex = new EmployeeNotFoundException("E-001");

            ApiErrorResponse body = handler.handleEmployeeNotFound(ex, request).getBody();

            assertThat(body.fieldErrors()).isEmpty();
        }
    }
}

