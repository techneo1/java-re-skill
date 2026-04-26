package com.srikanth.javareskill.repository;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.exception.DepartmentNotFoundException;
import com.srikanth.javareskill.exception.EmployeeNotFoundException;
import com.srikanth.javareskill.store.EmployeeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the generic repository hierarchy:
 *
 * <pre>
 * GenericRepository&lt;T, ID&gt;
 * ├── InMemoryRepository&lt;T, ID&gt;  (abstract)
 * │   ├── InMemoryEmployeeRepository   (T=Employee,    ID=EmployeeId)
 * │   └── InMemoryDepartmentRepository (T=Department,  ID=String)
 * ├── EmployeeRepository
 * └── DepartmentRepository
 * </pre>
 */
class GenericRepositoryTest {

    // =========================================================================
    // Shared builders
    // =========================================================================

    private static Employee buildEmployee(String id, String deptId,
                                          Role role, EmployeeStatus status) {
        return Employee.builder()
                .id(id)
                .name("Name-" + id)
                .email(id.toLowerCase() + "@example.com")
                .departmentId(deptId)
                .role(role)
                .salary(new BigDecimal("60000"))
                .status(status)
                .joiningDate(LocalDate.of(2023, 1, 1))
                .build();
    }

    private static Department buildDepartment(String id, String name, String location) {
        return Department.builder().id(id).name(name).location(location).build();
    }

    // =========================================================================
    // Generic CRUD — Employee repository (ID = EmployeeId)
    // =========================================================================

    @Nested
    class GenericCrudViaEmployeeRepositoryTest {

        private InMemoryEmployeeRepository repo;

        @BeforeEach
        void setUp() { repo = new InMemoryEmployeeRepository(); }

        @Test
        void save_then_findById_returnsEntity() {
            Employee e = buildEmployee("E001", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE);
            repo.save(e);
            Optional<Employee> found = repo.findById(new EmployeeId("E001"));
            assertTrue(found.isPresent());
            assertEquals(e, found.get());
        }

        @Test
        void save_duplicate_throwsIllegalArgumentException() {
            repo.save(buildEmployee("E001", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            assertThrows(IllegalArgumentException.class,
                    () -> repo.save(buildEmployee("E001", "D1", Role.MANAGER, EmployeeStatus.ACTIVE)));
        }

        @Test
        void findById_absent_returnsEmpty() {
            assertTrue(repo.findById(new EmployeeId("GHOST")).isEmpty());
        }

        @Test
        void findAll_preservesInsertionOrder() {
            Employee e1 = buildEmployee("E001", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE);
            Employee e2 = buildEmployee("E002", "D1", Role.MANAGER,  EmployeeStatus.ACTIVE);
            Employee e3 = buildEmployee("E003", "D2", Role.ANALYST,  EmployeeStatus.INACTIVE);
            repo.save(e1); repo.save(e2); repo.save(e3);
            assertEquals(List.of(e1, e2, e3), repo.findAll());
        }

        @Test
        void findAll_isUnmodifiable() {
            repo.save(buildEmployee("E001", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            assertThrows(UnsupportedOperationException.class,
                    () -> repo.findAll().add(
                            buildEmployee("E002", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE)));
        }

        @Test
        void update_replacesEntityAndPreservesListPosition() {
            repo.save(buildEmployee("E001", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            repo.save(buildEmployee("E002", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            repo.update(buildEmployee("E001", "D2", Role.SENIOR_ENGINEER, EmployeeStatus.ACTIVE));
            assertEquals("D2", repo.findById(new EmployeeId("E001")).get().getDepartmentId());
            assertEquals("E001", repo.findAll().get(0).getId());
        }

        @Test
        void update_missing_throwsEmployeeNotFoundException() {
            assertThrows(EmployeeNotFoundException.class,
                    () -> repo.update(
                            buildEmployee("GHOST", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE)));
        }

        @Test
        void deleteById_removesEntity() {
            repo.save(buildEmployee("E001", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            repo.deleteById(new EmployeeId("E001"));
            assertEquals(0, repo.count());
            assertTrue(repo.findById(new EmployeeId("E001")).isEmpty());
        }

        @Test
        void deleteById_missing_throwsEmployeeNotFoundException() {
            assertThrows(EmployeeNotFoundException.class,
                    () -> repo.deleteById(new EmployeeId("GHOST")));
        }

        @Test
        void existsById_presentAndAbsent() {
            repo.save(buildEmployee("E001", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            assertTrue(repo.existsById(new EmployeeId("E001")));
            assertFalse(repo.existsById(new EmployeeId("E002")));
        }

        @Test
        void count_afterSaveAndDelete() {
            repo.save(buildEmployee("E001", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            repo.save(buildEmployee("E002", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            repo.deleteById(new EmployeeId("E001"));
            assertEquals(1, repo.count());
        }

        @Test
        void save_null_throwsNullPointerException() {
            assertThrows(NullPointerException.class, () -> repo.save(null));
        }

        @Test
        void findById_null_throwsNullPointerException() {
            assertThrows(NullPointerException.class, () -> repo.findById(null));
        }
    }

    // =========================================================================
    // Generic CRUD — Department repository (ID = String)
    // =========================================================================

    @Nested
    class GenericCrudViaDepartmentRepositoryTest {

        private InMemoryDepartmentRepository repo;

        @BeforeEach
        void setUp() { repo = new InMemoryDepartmentRepository(); }

        @Test
        void save_then_findById_returnsEntity() {
            Department d = buildDepartment("D001", "Engineering", "New York");
            repo.save(d);
            assertTrue(repo.findById("D001").isPresent());
            assertEquals(d, repo.findById("D001").get());
        }

        @Test
        void save_duplicate_throwsIllegalArgumentException() {
            repo.save(buildDepartment("D001", "Engineering", "New York"));
            assertThrows(IllegalArgumentException.class,
                    () -> repo.save(buildDepartment("D001", "HR", "Boston")));
        }

        @Test
        void deleteById_missing_throwsDepartmentNotFoundException() {
            assertThrows(DepartmentNotFoundException.class, () -> repo.deleteById("GHOST"));
        }

        @Test
        void update_missing_throwsDepartmentNotFoundException() {
            assertThrows(DepartmentNotFoundException.class,
                    () -> repo.update(buildDepartment("GHOST", "X", "Y")));
        }

        @Test
        void existsById_stringKey() {
            repo.save(buildDepartment("D001", "Engineering", "NY"));
            assertTrue(repo.existsById("D001"));
            assertFalse(repo.existsById("D999"));
        }
    }

    // =========================================================================
    // Employee-specific queries
    // =========================================================================

    @Nested
    class EmployeeSpecificQueryTest {

        private InMemoryEmployeeRepository repo;

        @BeforeEach
        void setUp() {
            repo = new InMemoryEmployeeRepository();
            repo.save(buildEmployee("E001", "D1", Role.ENGINEER,        EmployeeStatus.ACTIVE));
            repo.save(buildEmployee("E002", "D1", Role.SENIOR_ENGINEER, EmployeeStatus.ACTIVE));
            repo.save(buildEmployee("E003", "D2", Role.MANAGER,         EmployeeStatus.ACTIVE));
            repo.save(buildEmployee("E004", "D2", Role.ENGINEER,        EmployeeStatus.INACTIVE));
            repo.save(buildEmployee("E005", "D3", Role.ANALYST,         EmployeeStatus.INACTIVE));
        }

        @Test
        void findByDepartmentId_returnsMatchingOnly() {
            List<Employee> d1 = repo.findByDepartmentId("D1");
            assertEquals(2, d1.size());
            assertTrue(d1.stream().allMatch(e -> "D1".equals(e.getDepartmentId())));
        }

        @Test
        void findByDepartmentId_noMatches_returnsEmpty() {
            assertTrue(repo.findByDepartmentId("D99").isEmpty());
        }

        @Test
        void findByStatus_active_returnsThree() {
            assertEquals(3, repo.findByStatus(EmployeeStatus.ACTIVE).size());
        }

        @Test
        void findByStatus_inactive_returnsTwo() {
            assertEquals(2, repo.findByStatus(EmployeeStatus.INACTIVE).size());
        }

        @Test
        void findByRole_engineer_returnsTwo() {
            assertEquals(2, repo.findByRole(Role.ENGINEER).size());
        }

        @Test
        void findByRole_noMatches_returnsEmpty() {
            assertTrue(repo.findByRole(Role.DIRECTOR).isEmpty());
        }

        @Test
        void findByDepartmentId_null_throwsNullPointerException() {
            assertThrows(NullPointerException.class, () -> repo.findByDepartmentId(null));
        }

        @Test
        void findByStatus_null_throwsNullPointerException() {
            assertThrows(NullPointerException.class, () -> repo.findByStatus(null));
        }
    }

    // =========================================================================
    // Department-specific queries
    // =========================================================================

    @Nested
    class DepartmentSpecificQueryTest {

        private InMemoryDepartmentRepository repo;

        @BeforeEach
        void setUp() {
            repo = new InMemoryDepartmentRepository();
            repo.save(buildDepartment("D001", "Engineering", "New York"));
            repo.save(buildDepartment("D002", "HR",          "Boston"));
            repo.save(buildDepartment("D003", "Finance",     "New York"));
        }

        @Test
        void findByLocation_caseInsensitive_returnsMatches() {
            List<Department> ny = repo.findByLocation("new york");
            assertEquals(2, ny.size());
            assertTrue(ny.stream().allMatch(d -> "New York".equalsIgnoreCase(d.getLocation())));
        }

        @Test
        void findByLocation_noMatches_returnsEmpty() {
            assertTrue(repo.findByLocation("Chicago").isEmpty());
        }

        @Test
        void findByName_caseInsensitive_returnsMatch() {
            Optional<Department> hr = repo.findByName("hr");
            assertTrue(hr.isPresent());
            assertEquals("D002", hr.get().getId());
        }

        @Test
        void findByName_absent_returnsEmpty() {
            assertTrue(repo.findByName("Legal").isEmpty());
        }

        @Test
        void findByLocation_null_throwsNullPointerException() {
            assertThrows(NullPointerException.class, () -> repo.findByLocation(null));
        }
    }

    // =========================================================================
    // Type-safety — both repos satisfy GenericRepository
    // =========================================================================

    @Nested
    class TypeSafetyTest {

        @Test
        void employeeRepository_isGenericRepository() {
            GenericRepository<Employee, EmployeeId> repo = new InMemoryEmployeeRepository();
            repo.save(buildEmployee("E001", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            assertEquals(1, repo.count());
        }

        @Test
        void departmentRepository_isGenericRepository() {
            GenericRepository<Department, String> repo = new InMemoryDepartmentRepository();
            repo.save(buildDepartment("D001", "Engineering", "NY"));
            assertEquals(1, repo.count());
        }

        @Test
        void employeeRepo_caseInsensitiveLookup_viaEmployeeId() {
            InMemoryEmployeeRepository repo = new InMemoryEmployeeRepository();
            repo.save(buildEmployee("E001", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            // "e001" and "E001" resolve to the same EmployeeId — proves the generic
            // repo delegates key equality to the ID type correctly
            assertTrue(repo.findById(new EmployeeId("e001")).isPresent());
        }

        @Test
        void bothRepos_independentState() {
            InMemoryEmployeeRepository   empRepo  = new InMemoryEmployeeRepository();
            InMemoryDepartmentRepository deptRepo = new InMemoryDepartmentRepository();
            empRepo.save(buildEmployee("E001", "D1", Role.ENGINEER, EmployeeStatus.ACTIVE));
            deptRepo.save(buildDepartment("D001", "Eng", "NY"));
            assertEquals(1, empRepo.count());
            assertEquals(1, deptRepo.count());
        }
    }
}
