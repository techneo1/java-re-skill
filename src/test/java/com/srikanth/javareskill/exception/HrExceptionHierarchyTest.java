package com.srikanth.javareskill.exception;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the custom exception hierarchy:
 *
 * <pre>
 * HrException
 * ├── ResourceNotFoundException
 * │   ├── EmployeeNotFoundException
 * │   └── DepartmentNotFoundException
 * └── BusinessRuleException
 *     ├── InvalidSalaryException
 *     └── DuplicateEmailException
 * </pre>
 */
class HrExceptionHierarchyTest {

    // =========================================================================
    // Hierarchy / instanceof checks
    // =========================================================================

    @Nested
    class HierarchyTest {

        @Test
        void employeeNotFoundException_isResourceNotFoundException() {
            assertInstanceOf(ResourceNotFoundException.class,
                    new EmployeeNotFoundException("E001"));
        }

        @Test
        void employeeNotFoundException_isHrException() {
            assertInstanceOf(HrException.class,
                    new EmployeeNotFoundException("E001"));
        }

        @Test
        void employeeNotFoundException_isRuntimeException() {
            assertInstanceOf(RuntimeException.class,
                    new EmployeeNotFoundException("E001"));
        }

        @Test
        void departmentNotFoundException_isResourceNotFoundException() {
            assertInstanceOf(ResourceNotFoundException.class,
                    new DepartmentNotFoundException("DEPT-1"));
        }

        @Test
        void departmentNotFoundException_isHrException() {
            assertInstanceOf(HrException.class,
                    new DepartmentNotFoundException("DEPT-1"));
        }

        @Test
        void invalidSalaryException_isBusinessRuleException() {
            assertInstanceOf(BusinessRuleException.class,
                    new InvalidSalaryException(new BigDecimal("-1"), "negative"));
        }

        @Test
        void invalidSalaryException_isHrException() {
            assertInstanceOf(HrException.class,
                    new InvalidSalaryException(new BigDecimal("-1"), "negative"));
        }

        @Test
        void duplicateEmailException_isBusinessRuleException() {
            assertInstanceOf(BusinessRuleException.class,
                    new DuplicateEmailException("a@b.com", "E001"));
        }

        @Test
        void duplicateEmailException_isHrException() {
            assertInstanceOf(HrException.class,
                    new DuplicateEmailException("a@b.com", "E001"));
        }

        @Test
        void resourceNotFound_and_businessRule_areDistinctBranches() {
            // A ResourceNotFoundException must NOT be a BusinessRuleException
            HrException resourceEx = new EmployeeNotFoundException("E001");
            assertFalse(resourceEx instanceof BusinessRuleException,
                    "ResourceNotFoundException should not be a BusinessRuleException");

            // A BusinessRuleException must NOT be a ResourceNotFoundException
            HrException businessEx = new InvalidSalaryException(BigDecimal.ONE, "too low");
            assertFalse(businessEx instanceof ResourceNotFoundException,
                    "BusinessRuleException should not be a ResourceNotFoundException");
        }
    }

    // =========================================================================
    // EmployeeNotFoundException
    // =========================================================================

    @Nested
    class EmployeeNotFoundExceptionTest {

        @Test
        void message_containsEmployeeId() {
            EmployeeNotFoundException ex = new EmployeeNotFoundException("E042");
            assertTrue(ex.getMessage().contains("E042"));
        }

        @Test
        void getEmployeeId_returnsId() {
            assertEquals("E042", new EmployeeNotFoundException("E042").getEmployeeId());
        }

        @Test
        void causeConstructor_preservesCause() {
            RuntimeException cause = new RuntimeException("db error");
            EmployeeNotFoundException ex = new EmployeeNotFoundException("E042", cause);
            assertSame(cause, ex.getCause());
            assertTrue(ex.getMessage().contains("E042"));
        }

        @Test
        void canBeCaughtAsHrException() {
            assertThrows(HrException.class, () -> {
                throw new EmployeeNotFoundException("E001");
            });
        }

        @Test
        void canBeCaughtAsResourceNotFoundException() {
            assertThrows(ResourceNotFoundException.class, () -> {
                throw new EmployeeNotFoundException("E001");
            });
        }
    }

    // =========================================================================
    // DepartmentNotFoundException
    // =========================================================================

    @Nested
    class DepartmentNotFoundExceptionTest {

        @Test
        void message_containsDepartmentId() {
            DepartmentNotFoundException ex = new DepartmentNotFoundException("DEPT-99");
            assertTrue(ex.getMessage().contains("DEPT-99"));
        }

        @Test
        void getDepartmentId_returnsId() {
            assertEquals("DEPT-99",
                    new DepartmentNotFoundException("DEPT-99").getDepartmentId());
        }

        @Test
        void causeConstructor_preservesCause() {
            RuntimeException cause = new RuntimeException("lookup failed");
            DepartmentNotFoundException ex = new DepartmentNotFoundException("DEPT-99", cause);
            assertSame(cause, ex.getCause());
            assertTrue(ex.getMessage().contains("DEPT-99"));
        }

        @Test
        void canBeCaughtAsHrException() {
            assertThrows(HrException.class, () -> {
                throw new DepartmentNotFoundException("DEPT-1");
            });
        }
    }

    // =========================================================================
    // InvalidSalaryException
    // =========================================================================

    @Nested
    class InvalidSalaryExceptionTest {

        @Test
        void message_containsValueAndReason() {
            InvalidSalaryException ex =
                    new InvalidSalaryException(new BigDecimal("-500"), "must not be negative");
            assertTrue(ex.getMessage().contains("-500"));
            assertTrue(ex.getMessage().contains("must not be negative"));
        }

        @Test
        void getInvalidValue_returnsValue() {
            BigDecimal bad = new BigDecimal("-500");
            assertEquals(bad, new InvalidSalaryException(bad, "negative").getInvalidValue());
        }

        @Test
        void nullSalary_allowedAsInvalidValue() {
            // null is a valid "bad" value to report (e.g. missing field)
            InvalidSalaryException ex = new InvalidSalaryException(null, "value is null");
            assertNull(ex.getInvalidValue());
            assertTrue(ex.getMessage().contains("null"));
        }

        @Test
        void causeConstructor_preservesCause() {
            RuntimeException cause = new RuntimeException("parse error");
            InvalidSalaryException ex =
                    new InvalidSalaryException(BigDecimal.ZERO, "cannot be zero", cause);
            assertSame(cause, ex.getCause());
        }

        @Test
        void canBeCaughtAsHrException() {
            assertThrows(HrException.class, () -> {
                throw new InvalidSalaryException(new BigDecimal("-1"), "negative");
            });
        }

        @Test
        void canBeCaughtAsBusinessRuleException() {
            assertThrows(BusinessRuleException.class, () -> {
                throw new InvalidSalaryException(new BigDecimal("-1"), "negative");
            });
        }
    }

    // =========================================================================
    // DuplicateEmailException
    // =========================================================================

    @Nested
    class DuplicateEmailExceptionTest {

        @Test
        void message_containsEmailAndExistingId() {
            DuplicateEmailException ex =
                    new DuplicateEmailException("alice@example.com", "E001");
            assertTrue(ex.getMessage().contains("alice@example.com"));
            assertTrue(ex.getMessage().contains("E001"));
        }

        @Test
        void getEmail_returnsEmail() {
            assertEquals("alice@example.com",
                    new DuplicateEmailException("alice@example.com", "E001").getEmail());
        }

        @Test
        void getExistingEmployeeId_returnsId() {
            assertEquals("E001",
                    new DuplicateEmailException("alice@example.com", "E001")
                            .getExistingEmployeeId());
        }

        @Test
        void causeConstructor_preservesCause() {
            RuntimeException cause = new RuntimeException("index violation");
            DuplicateEmailException ex =
                    new DuplicateEmailException("alice@example.com", "E001", cause);
            assertSame(cause, ex.getCause());
        }

        @Test
        void canBeCaughtAsHrException() {
            assertThrows(HrException.class, () -> {
                throw new DuplicateEmailException("x@y.com", "E002");
            });
        }

        @Test
        void canBeCaughtAsBusinessRuleException() {
            assertThrows(BusinessRuleException.class, () -> {
                throw new DuplicateEmailException("x@y.com", "E002");
            });
        }
    }

    // =========================================================================
    // Polymorphic catch — single handler for all HR exceptions
    // =========================================================================

    @Nested
    class PolymorphicCatchTest {

        private void dispatch(HrException ex) { throw ex; }

        @Test
        void catchHrException_handlesAllSubtypes() {
            HrException[] exceptions = {
                    new EmployeeNotFoundException("E001"),
                    new DepartmentNotFoundException("DEPT-1"),
                    new InvalidSalaryException(new BigDecimal("-1"), "negative"),
                    new DuplicateEmailException("a@b.com", "E001")
            };
            for (HrException ex : exceptions) {
                assertThrows(HrException.class, () -> dispatch(ex),
                        "Expected HrException for: " + ex.getClass().getSimpleName());
            }
        }

        @Test
        void catchResourceNotFoundException_doesNotCatchBusinessRuleExceptions() {
            // BusinessRuleExceptions must NOT leak through a ResourceNotFoundException catch
            assertThrows(InvalidSalaryException.class, () -> {
                try {
                    throw new InvalidSalaryException(BigDecimal.ZERO, "zero");
                } catch (ResourceNotFoundException ignored) {
                    fail("Should not be caught as ResourceNotFoundException");
                }
            });
        }
    }
}

