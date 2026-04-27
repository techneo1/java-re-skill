package com.srikanth.javareskill.service;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.EmployeeId;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.exception.DuplicateEmailException;
import com.srikanth.javareskill.exception.EmployeeNotFoundException;
import com.srikanth.javareskill.repository.EmployeeRepository;
import com.srikanth.javareskill.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EmployeeServiceImpl} using Mockito to isolate the service
 * from its repository and validation dependencies.
 *
 * <p>{@code @Mock} creates lightweight doubles for {@link EmployeeRepository} and
 * {@link ValidationService}; {@code @InjectMocks} wires them into
 * {@link EmployeeServiceImpl} via constructor injection.</p>
 *
 * <h2>SOLID principles demonstrated</h2>
 * <ul>
 *   <li><b>D – Dependency Inversion</b>: {@code EmployeeServiceImpl} depends on the
 *       {@code EmployeeRepository} and {@code ValidationService} abstractions,
 *       so Mockito can substitute any implementation at test time.</li>
 *   <li><b>S – Single Responsibility</b>: Each nested class tests one service
 *       concern (hire, find, mutate, filter).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @Mock
    private ValidationService validationService;

    @InjectMocks
    private EmployeeServiceImpl service;

    // -------------------------------------------------------------------------
    // Test-data helpers
    // -------------------------------------------------------------------------

    private static Employee buildEmployee(String id, String email,
                                          String departmentId, Role role,
                                          EmployeeStatus status) {
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

    private static final Employee ALICE =
            buildEmployee("E001", "alice@example.com", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE);

    private static final Employee BOB =
            buildEmployee("E002", "bob@example.com", "D1", Role.MANAGER, EmployeeStatus.ACTIVE);

    private static final Employee CAROL =
            buildEmployee("E003", "carol@example.com", "D2", Role.SENIOR_ENGINEER, EmployeeStatus.INACTIVE);

    // =========================================================================
    // hire()
    // =========================================================================

    @Nested
    @DisplayName("hire()")
    class HireTests {

        @Test
        @DisplayName("delegates email-uniqueness check to ValidationService")
        void hire_delegatesEmailValidation() {
            service.hire(ALICE);

            verify(validationService).validateEmailUniqueness(ALICE.getEmail());
        }

        @Test
        @DisplayName("saves employee to repository when validation passes")
        void hire_savesEmployee_whenValid() {
            service.hire(ALICE);

            verify(repository).save(ALICE);
        }

        @Test
        @DisplayName("throws NullPointerException for null employee")
        void hire_nullEmployee_throwsNpe() {
            assertThatNullPointerException()
                    .isThrownBy(() -> service.hire(null))
                    .withMessageContaining("employee must not be null");
        }

        @Test
        @DisplayName("propagates DuplicateEmailException from ValidationService")
        void hire_duplicateEmail_propagatesException() {
            doThrow(new DuplicateEmailException(ALICE.getEmail(), "E001"))
                    .when(validationService).validateEmailUniqueness(ALICE.getEmail());

            assertThatThrownBy(() -> service.hire(ALICE))
                    .isInstanceOf(DuplicateEmailException.class)
                    .hasFieldOrPropertyWithValue("email", ALICE.getEmail());
        }

        @Test
        @DisplayName("repository.save() is never called when validation fails")
        void hire_repositoryNotSaved_whenValidationFails() {
            doThrow(new DuplicateEmailException(ALICE.getEmail(), "E001"))
                    .when(validationService).validateEmailUniqueness(ALICE.getEmail());

            assertThatThrownBy(() -> service.hire(ALICE))
                    .isInstanceOf(DuplicateEmailException.class);

            verify(repository, never()).save(any());
        }
    }

    // =========================================================================
    // findById()
    // =========================================================================

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("returns employee wrapped in Optional when found")
        void findById_found_returnsOptionalWithEmployee() {
            EmployeeId id = new EmployeeId("E001");
            when(repository.findById(id)).thenReturn(Optional.of(ALICE));

            assertThat(service.findById(id)).contains(ALICE);
        }

        @Test
        @DisplayName("returns empty Optional when not found")
        void findById_notFound_returnsEmpty() {
            EmployeeId id = new EmployeeId("GHOST");
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThat(service.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("delegates to repository exactly once")
        void findById_delegatesToRepository() {
            EmployeeId id = new EmployeeId("E001");
            when(repository.findById(id)).thenReturn(Optional.of(ALICE));

            service.findById(id);

            verify(repository, times(1)).findById(id);
        }
    }

    // =========================================================================
    // findAll()
    // =========================================================================

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("returns all employees from repository")
        void findAll_returnsRepositoryResult() {
            when(repository.findAll()).thenReturn(List.of(ALICE, BOB));

            assertThat(service.findAll()).containsExactly(ALICE, BOB);
        }

        @Test
        @DisplayName("returns empty list when repository is empty")
        void findAll_emptyRepository_returnsEmptyList() {
            when(repository.findAll()).thenReturn(Collections.emptyList());

            assertThat(service.findAll()).isEmpty();
        }
    }

    // =========================================================================
    // update()
    // =========================================================================

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("delegates update to repository")
        void update_delegatesToRepository() {
            service.update(ALICE);

            verify(repository).update(ALICE);
        }

        @Test
        @DisplayName("propagates EmployeeNotFoundException from repository")
        void update_notFound_propagatesException() {
            doThrow(new EmployeeNotFoundException("GHOST"))
                    .when(repository).update(any());

            assertThatThrownBy(() -> service.update(ALICE))
                    .isInstanceOf(EmployeeNotFoundException.class);
        }
    }

    // =========================================================================
    // terminate()
    // =========================================================================

    @Nested
    @DisplayName("terminate()")
    class TerminateTests {

        @Test
        @DisplayName("delegates deleteById to repository")
        void terminate_delegatesToRepository() {
            EmployeeId id = new EmployeeId("E001");

            service.terminate(id);

            verify(repository).deleteById(id);
        }

        @Test
        @DisplayName("propagates EmployeeNotFoundException from repository")
        void terminate_notFound_propagatesException() {
            EmployeeId id = new EmployeeId("GHOST");
            doThrow(new EmployeeNotFoundException("GHOST"))
                    .when(repository).deleteById(id);

            assertThatThrownBy(() -> service.terminate(id))
                    .isInstanceOf(EmployeeNotFoundException.class);
        }
    }

    // =========================================================================
    // findByDepartment()
    // =========================================================================

    @Nested
    @DisplayName("findByDepartment()")
    class FindByDepartmentTests {

        @Test
        @DisplayName("returns employees for matching department")
        void findByDepartment_returnsMatchingEmployees() {
            when(repository.findByDepartmentId("D1")).thenReturn(List.of(ALICE, BOB));

            assertThat(service.findByDepartment("D1"))
                    .hasSize(2)
                    .containsExactly(ALICE, BOB);
        }

        @Test
        @DisplayName("returns empty list when no employees in department")
        void findByDepartment_noMatch_returnsEmpty() {
            when(repository.findByDepartmentId("D99")).thenReturn(Collections.emptyList());

            assertThat(service.findByDepartment("D99")).isEmpty();
        }

        @Test
        @DisplayName("passes departmentId to repository unchanged")
        void findByDepartment_passesDepartmentIdToRepository() {
            service.findByDepartment("D1");

            verify(repository).findByDepartmentId("D1");
        }
    }

    // =========================================================================
    // findByStatus()
    // =========================================================================

    @Nested
    @DisplayName("findByStatus()")
    class FindByStatusTests {

        @Test
        @DisplayName("returns employees with matching status")
        void findByStatus_active_returnsActiveEmployees() {
            when(repository.findByStatus(EmployeeStatus.ACTIVE)).thenReturn(List.of(ALICE, BOB));

            assertThat(service.findByStatus(EmployeeStatus.ACTIVE))
                    .hasSize(2)
                    .allMatch(e -> e.getStatus() == EmployeeStatus.ACTIVE);
        }

        @Test
        @DisplayName("returns empty list when no employees with given status")
        void findByStatus_noMatch_returnsEmpty() {
            when(repository.findByStatus(EmployeeStatus.INACTIVE)).thenReturn(Collections.emptyList());

            assertThat(service.findByStatus(EmployeeStatus.INACTIVE)).isEmpty();
        }

        @Test
        @DisplayName("delegates to repository with correct status")
        void findByStatus_delegatesToRepository() {
            service.findByStatus(EmployeeStatus.ACTIVE);

            verify(repository).findByStatus(EmployeeStatus.ACTIVE);
        }
    }

    // =========================================================================
    // findByRole()
    // =========================================================================

    @Nested
    @DisplayName("findByRole()")
    class FindByRoleTests {

        @Test
        @DisplayName("returns employees with matching role")
        void findByRole_engineer_returnsEngineers() {
            when(repository.findByRole(Role.ENGINEER)).thenReturn(List.of(ALICE));

            assertThat(service.findByRole(Role.ENGINEER))
                    .hasSize(1)
                    .allMatch(e -> e.getRole() == Role.ENGINEER);
        }

        @Test
        @DisplayName("returns empty list when no employees with given role")
        void findByRole_noMatch_returnsEmpty() {
            when(repository.findByRole(Role.DIRECTOR)).thenReturn(Collections.emptyList());

            assertThat(service.findByRole(Role.DIRECTOR)).isEmpty();
        }

        @Test
        @DisplayName("delegates to repository with correct role")
        void findByRole_delegatesToRepository() {
            service.findByRole(Role.MANAGER);

            verify(repository).findByRole(Role.MANAGER);
        }
    }

    // =========================================================================
    // headcount()
    // =========================================================================

    @Nested
    @DisplayName("headcount()")
    class HeadcountTests {

        @Test
        @DisplayName("returns count from repository")
        void headcount_returnsRepositoryCount() {
            when(repository.count()).thenReturn(42);

            assertThat(service.headcount()).isEqualTo(42);
        }

        @Test
        @DisplayName("returns zero when repository is empty")
        void headcount_emptyRepository_returnsZero() {
            when(repository.count()).thenReturn(0);

            assertThat(service.headcount()).isZero();
        }

        @Test
        @DisplayName("delegates to repository.count() exactly once")
        void headcount_delegatesToRepository() {
            service.headcount();

            verify(repository, times(1)).count();
        }
    }
}
