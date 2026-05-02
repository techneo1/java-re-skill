package com.srikanth.javareskill.exception;

import com.srikanth.javareskill.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception → HTTP response mapping for the entire REST API.
 *
 * <h2>How Spring resolves handlers</h2>
 * <p>Spring walks the {@code @ExceptionHandler} methods from the most-specific
 * exception type to the least-specific.  The ordering below mirrors that
 * specificity so the intent is obvious at a glance:</p>
 * <ol>
 *   <li>Bean Validation failures        ({@code 400})</li>
 *   <li>Malformed request body          ({@code 400})</li>
 *   <li>Missing / wrong-type parameters ({@code 400})</li>
 *   <li>Duplicate email                 ({@code 409})</li>
 *   <li>Invalid salary                  ({@code 422})</li>
 *   <li>Any other business-rule breach  ({@code 422})</li>
 *   <li>Employee not found              ({@code 404})</li>
 *   <li>Department not found            ({@code 404})</li>
 *   <li>Any other resource not found    ({@code 404})</li>
 *   <li>Any other {@code HrException}   ({@code 500})</li>
 *   <li>Spring's own 404 (no route)     ({@code 404})</li>
 *   <li>Catch-all {@code Exception}     ({@code 500})</li>
 * </ol>
 *
 * <h2>Logging strategy</h2>
 * <ul>
 *   <li>Client errors (4xx) → {@code WARN} (no stack-trace; the client sent bad data)</li>
 *   <li>Server errors (5xx) → {@code ERROR} with full stack-trace</li>
 * </ul>
 *
 * <h2>Error envelope</h2>
 * <p>Every response body is an {@link ApiErrorResponse} so clients can parse
 * errors uniformly without branching on status codes.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =========================================================================
    // 400 Bad Request
    // =========================================================================

    /**
     * Handles {@code @Valid} / {@code @Validated} violations on request bodies.
     *
     * <p>Groups all field-level constraint violations by field name and returns
     * them in the {@code fieldErrors} map so the client knows exactly which
     * fields to fix.</p>
     *
     * <p>Example response body:
     * <pre>{@code
     * {
     *   "status": 400,
     *   "error":  "Bad Request",
     *   "message": "Validation failed — see fieldErrors for details",
     *   "fieldErrors": {
     *     "email":  ["must be a well-formed email address"],
     *     "salary": ["must not be negative"]
     *   }
     * }
     * }</pre></p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        // Group violations: field → list-of-messages
        Map<String, List<String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        fe -> fe.getField(),
                        LinkedHashMap::new,
                        Collectors.mapping(fe -> fe.getDefaultMessage(), Collectors.toList())
                ));

        log.warn("Validation failed on {}: {}", request.getRequestURI(), fieldErrors);

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.validationError(request.getRequestURI(), fieldErrors));
    }

    /**
     * Handles malformed JSON bodies (e.g. invalid enum value, wrong data type,
     * missing required fields that Jackson cannot bind).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Unreadable request body on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.of(
                        400, "Bad Request",
                        "Request body is missing or cannot be parsed: " + rootCause(ex),
                        request.getRequestURI()));
    }

    /**
     * Handles missing required query / path parameters.
     *
     * <p>Example: {@code GET /payroll} without the required {@code month} parameter.</p>
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        log.warn("Missing request parameter '{}' on {}", ex.getParameterName(), request.getRequestURI());

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.of(
                        400, "Bad Request",
                        "Required parameter '" + ex.getParameterName() + "' is missing",
                        request.getRequestURI()));
    }

    /**
     * Handles type-conversion failures for query / path parameters
     * (e.g. passing {@code "abc"} where an {@code int} is expected).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String msg = "Parameter '%s' must be of type %s; received: '%s'"
                .formatted(ex.getName(), expected, ex.getValue());

        log.warn("Type mismatch on {}: {}", request.getRequestURI(), msg);

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.of(400, "Bad Request", msg, request.getRequestURI()));
    }

    // =========================================================================
    // 409 Conflict
    // =========================================================================

    /**
     * Handles {@link DuplicateEmailException} — e-mail already registered.
     *
     * <p>HTTP 409 Conflict is more precise than 422 here because the client
     * supplied a valid value that conflicts with an existing resource.</p>
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex, HttpServletRequest request) {

        log.warn("Duplicate email '{}' attempted on {}", ex.getEmail(), request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        409, "Conflict",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    // =========================================================================
    // 422 Unprocessable Entity  (business-rule violations)
    // =========================================================================

    /**
     * Handles {@link InvalidSalaryException} — salary outside permitted range.
     */
    @ExceptionHandler(InvalidSalaryException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSalary(
            InvalidSalaryException ex, HttpServletRequest request) {

        log.warn("Invalid salary on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .unprocessableEntity()
                .body(ApiErrorResponse.of(
                        422, "Unprocessable Entity",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    /**
     * Catch-all for any other {@link BusinessRuleException} not matched above.
     *
     * <p>Examples: custom rule extensions added in future that extend
     * {@code BusinessRuleException} directly.</p>
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(
            BusinessRuleException ex, HttpServletRequest request) {

        log.warn("Business rule violation [{}] on {}: {}",
                ex.getErrorCode().getCode(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .unprocessableEntity()
                .body(ApiErrorResponse.of(
                        422, "Unprocessable Entity",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    // =========================================================================
    // 404 Not Found
    // =========================================================================

    /**
     * Handles {@link EmployeeNotFoundException}.
     */
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEmployeeNotFound(
            EmployeeNotFoundException ex, HttpServletRequest request) {

        log.warn("Employee not found [id={}] on {}", ex.getEmployeeId(), request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        404, "Not Found",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    /**
     * Handles {@link DepartmentNotFoundException}.
     */
    @ExceptionHandler(DepartmentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleDepartmentNotFound(
            DepartmentNotFoundException ex, HttpServletRequest request) {

        log.warn("Department not found on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        404, "Not Found",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    /**
     * Catch-all for any other {@link ResourceNotFoundException} not matched above.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found [{}] on {}: {}",
                ex.getErrorCode().getCode(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        404, "Not Found",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    /**
     * Handles Spring MVC's own "no handler found" exception
     * (e.g. {@code GET /unknown-path}).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoRoute(
            NoResourceFoundException ex, HttpServletRequest request) {

        log.warn("No route found: {} {}", request.getMethod(), request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        404, "Not Found",
                        "No endpoint found for: " + request.getMethod() + " " + request.getRequestURI(),
                        request.getRequestURI()));
    }

    // =========================================================================
    // 500 Internal Server Error — catch-alls
    // =========================================================================

    /**
     * Catch-all for any other {@link HrException} not matched by a more-specific
     * handler above (e.g. {@link ConfigurationException}).
     *
     * <p>Returns 500 and logs with full stack-trace.</p>
     */
    @ExceptionHandler(HrException.class)
    public ResponseEntity<ApiErrorResponse> handleHrException(
            HrException ex, HttpServletRequest request) {

        log.error("Unexpected HR exception [{}] on {}: {}",
                ex.getErrorCode().getCode(), request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity
                .internalServerError()
                .body(ApiErrorResponse.of(
                        500, "Internal Server Error",
                        "An unexpected error occurred. Reference code: " + ex.getErrorCode().getCode(),
                        request.getRequestURI()));
    }

    /**
     * Last-resort catch-all for any {@link Exception} not handled above.
     *
     * <p>Hides implementation details from the response body; the full stack-trace
     * is logged at ERROR level for diagnosis.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity
                .internalServerError()
                .body(ApiErrorResponse.of(
                        500, "Internal Server Error",
                        "An unexpected internal error occurred. Please try again later.",
                        request.getRequestURI()));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Extracts the innermost cause message to avoid exposing Jackson internals. */
    private static String rootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        // Trim verbose Jackson path expressions like " (through reference chain ...)"
        if (msg != null && msg.contains(" (through reference chain")) {
            msg = msg.substring(0, msg.indexOf(" (through reference chain"));
        }
        return msg != null ? msg : cause.getClass().getSimpleName();
    }
}

