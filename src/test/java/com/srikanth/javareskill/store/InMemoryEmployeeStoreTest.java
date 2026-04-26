package com.srikanth.javareskill.store;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryEmployeeStoreTest {

    private InMemoryEmployeeStore store;

    private static Employee buildEmployee(String id, String name) {
        return Employee.builder()
                .id(id)
                .name(name)
                .email(name.toLowerCase().replace(" ", ".") + "@example.com")
                .departmentId("DEPT-1")
                .role(Role.ENGINEER)
                .salary(new BigDecimal("60000"))
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2023, 1, 15))
                .build();
    }

    @BeforeEach
    void setUp() {
        store = new InMemoryEmployeeStore();
    }

    // --- add ---

    @Test
    void add_singleEmployee_sizeIsOne() {
        store.add(buildEmployee("E001", "Alice"));
        assertEquals(1, store.size());
    }

    @Test
    void add_duplicateId_throwsIllegalArgumentException() {
        store.add(buildEmployee("E001", "Alice"));
        assertThrows(IllegalArgumentException.class,
                () -> store.add(buildEmployee("E001", "Bob")));
    }

    @Test
    void add_nullEmployee_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> store.add(null));
    }

    // --- findById ---

    @Test
    void findById_existingId_returnsEmployee() {
        Employee alice = buildEmployee("E001", "Alice");
        store.add(alice);
        Optional<Employee> result = store.findById(new EmployeeId("E001"));
        assertTrue(result.isPresent());
        assertEquals(alice, result.get());
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        Optional<Employee> result = store.findById(new EmployeeId("UNKNOWN"));
        assertTrue(result.isEmpty());
    }

    @Test
    void findById_nullId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> store.findById(null));
    }

    // --- findAll ---

    @Test
    void findAll_emptyStore_returnsEmptyList() {
        assertTrue(store.findAll().isEmpty());
    }

    @Test
    void findAll_preservesInsertionOrder() {
        Employee e1 = buildEmployee("E001", "Alice");
        Employee e2 = buildEmployee("E002", "Bob");
        Employee e3 = buildEmployee("E003", "Carol");
        store.add(e1);
        store.add(e2);
        store.add(e3);

        List<Employee> all = store.findAll();
        assertEquals(List.of(e1, e2, e3), all);
    }

    @Test
    void findAll_returnsUnmodifiableList() {
        store.add(buildEmployee("E001", "Alice"));
        List<Employee> all = store.findAll();
        assertThrows(UnsupportedOperationException.class,
                () -> all.add(buildEmployee("E002", "Bob")));
    }

    // --- update ---

    @Test
    void update_existingEmployee_updatesInBothStructures() {
        store.add(buildEmployee("E001", "Alice"));
        Employee updated = Employee.builder()
                .id("E001")
                .name("Alice Updated")
                .email("alice.updated@example.com")
                .departmentId("DEPT-2")
                .role(Role.SENIOR_ENGINEER)
                .salary(new BigDecimal("80000"))
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2023, 1, 15))
                .build();

        store.update(updated);

        assertEquals("Alice Updated", store.findById(new EmployeeId("E001")).get().getName());
        assertEquals("Alice Updated", store.findAll().get(0).getName());
    }

    @Test
    void update_preservesPositionInList() {
        Employee e1 = buildEmployee("E001", "Alice");
        Employee e2 = buildEmployee("E002", "Bob");
        Employee e3 = buildEmployee("E003", "Carol");
        store.add(e1);
        store.add(e2);
        store.add(e3);

        Employee updatedBob = Employee.builder()
                .id("E002").name("Bob Updated").email("bob.updated@example.com")
                .departmentId("DEPT-1").role(Role.MANAGER).salary(new BigDecimal("90000"))
                .status(EmployeeStatus.ACTIVE).joiningDate(LocalDate.of(2022, 5, 1))
                .build();
        store.update(updatedBob);

        List<Employee> all = store.findAll();
        assertEquals(3, all.size());
        assertEquals("E001", all.get(0).getId());
        assertEquals("E002", all.get(1).getId());
        assertEquals("E003", all.get(2).getId());
        assertEquals("Bob Updated", all.get(1).getName());
    }

    @Test
    void update_unknownId_throwsEmployeeNotFoundException() {
        assertThrows(EmployeeNotFoundException.class,
                () -> store.update(buildEmployee("GHOST", "Ghost")));
    }

    // --- remove ---

    @Test
    void remove_existingEmployee_removesFromBothStructures() {
        store.add(buildEmployee("E001", "Alice"));
        store.remove(new EmployeeId("E001"));
        assertEquals(0, store.size());
        assertTrue(store.findById(new EmployeeId("E001")).isEmpty());
        assertTrue(store.findAll().isEmpty());
    }

    @Test
    void remove_unknownId_throwsEmployeeNotFoundException() {
        assertThrows(EmployeeNotFoundException.class, () -> store.remove(new EmployeeId("GHOST")));
    }

    @Test
    void remove_nullId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> store.remove(null));
    }

    // --- size ---

    @Test
    void size_afterMultipleAddAndRemove_isCorrect() {
        store.add(buildEmployee("E001", "Alice"));
        store.add(buildEmployee("E002", "Bob"));
        store.remove(new EmployeeId("E001"));
        assertEquals(1, store.size());
    }
}

