package com.srikanth.javareskill.service;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.EmployeeId;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.exception.DuplicateEmailException;
import com.srikanth.javareskill.exception.EmployeeNotFoundException;
import com.srikanth.javareskill.repository.inmemory.InMemoryDepartmentRepository;
import com.srikanth.javareskill.repository.inmemory.InMemoryEmployeeRepository;
import com.srikanth.javareskill.service.impl.EmployeeServiceImpl;
import com.srikanth.javareskill.service.impl.ValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link EmployeeServiceImpl} backed by an {@link InMemoryEmployeeRepository}.
 *
 * <p>Covers business-rule enforcement (duplicate e-mail) as well as delegation
 * to the repository for CRUD and query operations.</p>
 *
 * <h2>SOLID demonstrated</h2>
 * <ul>
 *   <li><b>D – Dependency Inversion</b>: {@code EmployeeServiceImpl} is constructed
 *       with a {@link ValidationService} interface (supplied here as
 *       {@code ValidationServiceImpl}).  Swapping the implementation (e.g. a no-op
 *       stub for isolated unit tests) requires no changes to
 *       {@code EmployeeServiceImpl}.</li>
 * </ul>
 */
class EmployeeServiceTest {

    private EmployeeService service;

    private static Employee buildEmployee(String id, String email, String departmentId,
                                          Role role, EmployeeStatus status) {
        return Employee.builder()
                .id(id)
                .name("Name-" + id)
                .email(email)
                .departmentId(departmentId)
                .role(role)
                .salary(new BigDecimal("70000"))
                .status(status)
                .joiningDate(LocalDate.of(2023, 6, 1))
                .build();
    }

    @BeforeEach
    void setUp() {
        // DIP: wire concrete implementations through their abstractions.
        InMemoryEmployeeRepository   empRepo  = new InMemoryEmployeeRepository();
        InMemoryDepartmentRepository deptRepo = new InMemoryDepartmentRepository();
        ValidationService validationService   = new ValidationServiceImpl(empRepo, deptRepo);
        service = new EmployeeServiceImpl(empRepo, validationService);
    }

    // =========================================================================
    // hire
    // =========================================================================

    @Nested
    class HireTest {

        @Test
        void hire_newEmployee_increasesHeadcount() {
            service.hire(buildEmployee("E001", "alice@example.com", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            assertThat(service.headcount()).isEqualTo(1);
        }

        @Test
        void hire_duplicateEmail_throwsDuplicateEmailException() {
            service.hire(buildEmployee("E001", "alice@example.com", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            assertThatThrownBy(() ->
                    service.hire(buildEmployee("E002", "alice@example.com", "D1", Role.MANAGER, EmployeeStatus.ACTIVE)))
                    .isInstanceOf(DuplicateEmailException.class);
        }

        @Test
        void hire_duplicateEmail_caseInsensitive() {
            service.hire(buildEmployee("E001", "Alice@Example.COM", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            assertThatThrownBy(() ->
                    service.hire(buildEmployee("E002", "alice@example.com", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE)))
                    .isInstanceOf(DuplicateEmailException.class);
        }

        @Test
        void hire_duplicateId_throwsIllegalArgumentException() {
            service.hire(buildEmployee("E001", "alice@example.com", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            assertThatThrownBy(() ->
                    service.hire(buildEmployee("E001", "other@example.com", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // findById / findAll
    // =========================================================================

    @Nested
    class QueryTest {

        @Test
        void findById_existingEmployee_returnsEmployee() {
            Employee e = buildEmployee("E001", "alice@example.com", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE);
            service.hire(e);
            assertThat(service.findById(new EmployeeId("E001"))).contains(e);
        }

        @Test
        void findById_unknown_returnsEmpty() {
            assertThat(service.findById(new EmployeeId("GHOST"))).isEmpty();
        }

        @Test
        void findAll_preservesInsertionOrder() {
            Employee e1 = buildEmployee("E001", "e1@example.com", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE);
            Employee e2 = buildEmployee("E002", "e2@example.com", "D1", Role.MANAGER,  EmployeeStatus.ACTIVE);
            service.hire(e1);
            service.hire(e2);
            assertThat(service.findAll()).containsExactly(e1, e2);
        }
    }

    // =========================================================================
    // update / terminate
    // =========================================================================

    @Nested
    class MutationTest {

        @Test
        void update_changesStoredRecord() {
            service.hire(buildEmployee("E001", "e1@example.com", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            Employee promoted = buildEmployee("E001", "e1@example.com", "D1", Role.SENIOR_ENGINEER, EmployeeStatus.ACTIVE);
            service.update(promoted);
            assertThat(service.findById(new EmployeeId("E001")).map(Employee::getRole))
                    .contains(Role.SENIOR_ENGINEER);
        }

        @Test
        void update_missing_throwsEmployeeNotFoundException() {
            assertThatThrownBy(() ->
                    service.update(buildEmployee("GHOST", "g@example.com", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE)))
                    .isInstanceOf(EmployeeNotFoundException.class);
        }

        @Test
        void terminate_removesEmployee() {
            service.hire(buildEmployee("E001", "e1@example.com", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            service.terminate(new EmployeeId("E001"));
            assertThat(service.headcount()).isZero();
        }

        @Test
        void terminate_missing_throwsEmployeeNotFoundException() {
            assertThatThrownBy(() -> service.terminate(new EmployeeId("GHOST")))
                    .isInstanceOf(EmployeeNotFoundException.class);
        }
    }

    // =========================================================================
    // Filtered queries
    // =========================================================================

    @Nested
    class FilteredQueryTest {

        @BeforeEach
        void populate() {
            service.hire(buildEmployee("E001", "e1@example.com", "D1", Role.ENGINEER,        EmployeeStatus.ACTIVE));
            service.hire(buildEmployee("E002", "e2@example.com", "D1", Role.SENIOR_ENGINEER, EmployeeStatus.ACTIVE));
            service.hire(buildEmployee("E003", "e3@example.com", "D2", Role.MANAGER,         EmployeeStatus.ACTIVE));
            service.hire(buildEmployee("E004", "e4@example.com", "D2", Role.ENGINEER,        EmployeeStatus.INACTIVE));
        }

        @Test
        void findByDepartment_returnsMatchingOnly() {
            List<Employee> d1 = service.findByDepartment("D1");
            assertThat(d1).hasSize(2).allMatch(e -> "D1".equals(e.getDepartmentId()));
        }

        @Test
        void findByStatus_active_returnsThree() {
            assertThat(service.findByStatus(EmployeeStatus.ACTIVE)).hasSize(3);
        }

        @Test
        void findByRole_engineer_returnsTwo() {
            assertThat(service.findByRole(Role.ENGINEER)).hasSize(2);
        }
    }
}

