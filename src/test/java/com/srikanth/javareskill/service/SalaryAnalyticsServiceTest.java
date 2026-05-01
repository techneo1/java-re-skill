package com.srikanth.javareskill.service;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.service.impl.SalaryAnalyticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SalaryAnalyticsServiceImpl}.
 *
 * <p>Uses Mockito to mock {@link EmployeeService#findAll()} so that every
 * test controls exactly which employees are in the dataset.  Each nested
 * class focuses on one analytics method.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SalaryAnalyticsServiceImpl")
class SalaryAnalyticsServiceTest {

    @Mock
    private EmployeeService employeeService;

    private SalaryAnalyticsService analytics;

    // -------------------------------------------------------------------------
    // Test-data helpers
    // -------------------------------------------------------------------------

    private static Employee emp(String id, String deptId, Role role, String salary) {
        return emp(id, deptId, role, salary, EmployeeStatus.ACTIVE);
    }

    private static Employee emp(String id, String deptId, Role role, String salary,
                                EmployeeStatus status) {
        return Employee.builder()
                .id(id)
                .name("Name-" + id)
                .email(id.toLowerCase() + "@example.com")
                .departmentId(deptId)
                .role(role)
                .salary(new BigDecimal(salary))
                .status(status)
                .joiningDate(LocalDate.of(2023, 1, 1))
                .build();
    }

    // A diverse 7-employee dataset used by most tests
    private static final Employee E1 = emp("E001", "D1", Role.ENGINEER,        "50000");
    private static final Employee E2 = emp("E002", "D1", Role.SENIOR_ENGINEER, "80000");
    private static final Employee E3 = emp("E003", "D2", Role.MANAGER,         "95000");
    private static final Employee E4 = emp("E004", "D2", Role.ENGINEER,        "55000");
    private static final Employee E5 = emp("E005", "D3", Role.DIRECTOR,       "150000");
    private static final Employee E6 = emp("E006", "D1", Role.ANALYST,         "60000");
    private static final Employee E7 = emp("E007", "D3", Role.ENGINEER,        "52000");

    private static final List<Employee> ALL = List.of(E1, E2, E3, E4, E5, E6, E7);

    @BeforeEach
    void setUp() {
        analytics = new SalaryAnalyticsServiceImpl(employeeService);
    }

    // =========================================================================
    // Constructor
    // =========================================================================

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("null employeeService throws NullPointerException")
        void nullEmployeeService_throwsNpe() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new SalaryAnalyticsServiceImpl(null))
                    .withMessageContaining("employeeService must not be null");
        }
    }

    // =========================================================================
    // groupByDepartment()
    // =========================================================================

    @Nested
    @DisplayName("groupByDepartment()")
    class GroupByDepartmentTests {

        @Test
        @DisplayName("groups employees into correct department buckets")
        void groupsByDepartmentCorrectly() {
            when(employeeService.findAll()).thenReturn(ALL);

            Map<String, List<Employee>> grouped = analytics.groupByDepartment();

            assertThat(grouped).hasSize(3);
            assertThat(grouped.get("D1")).containsExactlyInAnyOrder(E1, E2, E6);
            assertThat(grouped.get("D2")).containsExactlyInAnyOrder(E3, E4);
            assertThat(grouped.get("D3")).containsExactlyInAnyOrder(E5, E7);
        }

        @Test
        @DisplayName("returns empty map when there are no employees")
        void emptyEmployees_returnsEmptyMap() {
            when(employeeService.findAll()).thenReturn(Collections.emptyList());

            assertThat(analytics.groupByDepartment()).isEmpty();
        }

        @Test
        @DisplayName("single employee creates a single-entry map")
        void singleEmployee_singleDepartment() {
            when(employeeService.findAll()).thenReturn(List.of(E1));

            Map<String, List<Employee>> grouped = analytics.groupByDepartment();

            assertThat(grouped).hasSize(1);
            assertThat(grouped.get("D1")).containsExactly(E1);
        }

        @Test
        @DisplayName("department lists are unmodifiable")
        void departmentLists_areUnmodifiable() {
            when(employeeService.findAll()).thenReturn(List.of(E1));

            List<Employee> d1 = analytics.groupByDepartment().get("D1");

            assertThatThrownBy(() -> d1.add(E2))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // =========================================================================
    // topNHighestSalaries()
    // =========================================================================

    @Nested
    @DisplayName("topNHighestSalaries()")
    class TopNHighestSalariesTests {

        @Test
        @DisplayName("top 5 returns the five highest-paid employees in descending order")
        void top5_returnsCorrectOrderAndSize() {
            when(employeeService.findAll()).thenReturn(ALL);

            List<Employee> top5 = analytics.top5HighestSalaries();

            assertThat(top5).hasSize(5);
            // Expected order: E5(150k) > E3(95k) > E2(80k) > E6(60k) > E4(55k)
            assertThat(top5).extracting(Employee::getId)
                    .containsExactly("E005", "E003", "E002", "E006", "E004");
        }

        @Test
        @DisplayName("top 3 returns exactly 3 highest salaries")
        void top3_returnsThree() {
            when(employeeService.findAll()).thenReturn(ALL);

            List<Employee> top3 = analytics.topNHighestSalaries(3);

            assertThat(top3).hasSize(3);
            assertThat(top3.get(0).getSalary()).isEqualByComparingTo("150000");
            assertThat(top3.get(1).getSalary()).isEqualByComparingTo("95000");
            assertThat(top3.get(2).getSalary()).isEqualByComparingTo("80000");
        }

        @Test
        @DisplayName("top 1 returns the single highest-paid employee")
        void top1_returnsSingleHighest() {
            when(employeeService.findAll()).thenReturn(ALL);

            List<Employee> top1 = analytics.topNHighestSalaries(1);

            assertThat(top1).hasSize(1);
            assertThat(top1.get(0)).isEqualTo(E5);
        }

        @Test
        @DisplayName("n > total employees returns all employees sorted descending")
        void nGreaterThanTotal_returnsAllSorted() {
            when(employeeService.findAll()).thenReturn(ALL);

            List<Employee> topAll = analytics.topNHighestSalaries(100);

            assertThat(topAll).hasSize(7);
            // Verify descending order
            for (int i = 1; i < topAll.size(); i++) {
                assertThat(topAll.get(i - 1).getSalary())
                        .isGreaterThanOrEqualTo(topAll.get(i).getSalary());
            }
        }

        @Test
        @DisplayName("empty employee list returns empty list")
        void emptyEmployees_returnsEmptyList() {
            when(employeeService.findAll()).thenReturn(Collections.emptyList());

            assertThat(analytics.topNHighestSalaries(5)).isEmpty();
        }

        @Test
        @DisplayName("n = 0 throws IllegalArgumentException")
        void zero_throwsIllegalArgumentException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> analytics.topNHighestSalaries(0));
        }

        @Test
        @DisplayName("negative n throws IllegalArgumentException")
        void negative_throwsIllegalArgumentException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> analytics.topNHighestSalaries(-1));
        }

        @Test
        @DisplayName("result list is unmodifiable")
        void resultList_isUnmodifiable() {
            when(employeeService.findAll()).thenReturn(List.of(E1));

            List<Employee> result = analytics.topNHighestSalaries(5);

            assertThatThrownBy(() -> result.add(E2))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // =========================================================================
    // averageSalaryPerRole()
    // =========================================================================

    @Nested
    @DisplayName("averageSalaryPerRole()")
    class AverageSalaryPerRoleTests {

        @Test
        @DisplayName("computes correct averages for roles with multiple employees")
        void multipleEmployeesPerRole_correctAverages() {
            when(employeeService.findAll()).thenReturn(ALL);

            Map<Role, BigDecimal> averages = analytics.averageSalaryPerRole();

            // ENGINEER: (50000 + 55000 + 52000) / 3 = 52333.33
            assertThat(averages.get(Role.ENGINEER))
                    .isEqualByComparingTo("52333.33");

            // SENIOR_ENGINEER: 80000 / 1
            assertThat(averages.get(Role.SENIOR_ENGINEER))
                    .isEqualByComparingTo("80000.00");

            // MANAGER: 95000 / 1
            assertThat(averages.get(Role.MANAGER))
                    .isEqualByComparingTo("95000.00");

            // DIRECTOR: 150000 / 1
            assertThat(averages.get(Role.DIRECTOR))
                    .isEqualByComparingTo("150000.00");

            // ANALYST: 60000 / 1
            assertThat(averages.get(Role.ANALYST))
                    .isEqualByComparingTo("60000.00");
        }

        @Test
        @DisplayName("roles with no employees are absent from the map")
        void roleWithNoEmployees_absent() {
            when(employeeService.findAll()).thenReturn(ALL);

            Map<Role, BigDecimal> averages = analytics.averageSalaryPerRole();

            // HR and SENIOR_MANAGER have no employees in our dataset
            assertThat(averages).doesNotContainKey(Role.HR);
            assertThat(averages).doesNotContainKey(Role.SENIOR_MANAGER);
        }

        @Test
        @DisplayName("map size equals the number of distinct roles present")
        void mapSize_matchesDistinctRoles() {
            when(employeeService.findAll()).thenReturn(ALL);

            assertThat(analytics.averageSalaryPerRole()).hasSize(5);
        }

        @Test
        @DisplayName("returns empty map when there are no employees")
        void emptyEmployees_returnsEmptyMap() {
            when(employeeService.findAll()).thenReturn(Collections.emptyList());

            assertThat(analytics.averageSalaryPerRole()).isEmpty();
        }

        @Test
        @DisplayName("single employee → average equals that employee's salary")
        void singleEmployee_averageEqualsSalary() {
            when(employeeService.findAll()).thenReturn(List.of(E5));

            Map<Role, BigDecimal> averages = analytics.averageSalaryPerRole();

            assertThat(averages).hasSize(1);
            assertThat(averages.get(Role.DIRECTOR)).isEqualByComparingTo("150000.00");
        }

        @Test
        @DisplayName("average is rounded to 2 decimal places (HALF_UP)")
        void averageScale_isTwoDecimalPlaces() {
            when(employeeService.findAll()).thenReturn(ALL);

            BigDecimal engineerAvg = analytics.averageSalaryPerRole().get(Role.ENGINEER);

            assertThat(engineerAvg.scale()).isEqualTo(2);
        }
    }

    // =========================================================================
    // maxSalary()
    // =========================================================================

    @Nested
    @DisplayName("maxSalary()")
    class MaxSalaryTests {

        @Test
        @DisplayName("returns the highest salary across all employees")
        void returnsHighestSalary() {
            when(employeeService.findAll()).thenReturn(ALL);

            assertThat(analytics.maxSalary()).isEqualByComparingTo("150000");
        }

        @Test
        @DisplayName("returns ZERO when there are no employees")
        void emptyEmployees_returnsZero() {
            when(employeeService.findAll()).thenReturn(Collections.emptyList());

            assertThat(analytics.maxSalary()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("single employee → max equals that employee's salary")
        void singleEmployee_maxEqualsSalary() {
            when(employeeService.findAll()).thenReturn(List.of(E1));

            assertThat(analytics.maxSalary()).isEqualByComparingTo("50000");
        }
    }

    // =========================================================================
    // minSalary()
    // =========================================================================

    @Nested
    @DisplayName("minSalary()")
    class MinSalaryTests {

        @Test
        @DisplayName("returns the lowest salary across all employees")
        void returnsLowestSalary() {
            when(employeeService.findAll()).thenReturn(ALL);

            assertThat(analytics.minSalary()).isEqualByComparingTo("50000");
        }

        @Test
        @DisplayName("returns ZERO when there are no employees")
        void emptyEmployees_returnsZero() {
            when(employeeService.findAll()).thenReturn(Collections.emptyList());

            assertThat(analytics.minSalary()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("single employee → min equals that employee's salary")
        void singleEmployee_minEqualsSalary() {
            when(employeeService.findAll()).thenReturn(List.of(E5));

            assertThat(analytics.minSalary()).isEqualByComparingTo("150000");
        }
    }

    // =========================================================================
    // totalSalaryBill()
    // =========================================================================

    @Nested
    @DisplayName("totalSalaryBill()")
    class TotalSalaryBillTests {

        @Test
        @DisplayName("returns sum of all salaries")
        void returnsSumOfAllSalaries() {
            when(employeeService.findAll()).thenReturn(ALL);

            // 50000 + 80000 + 95000 + 55000 + 150000 + 60000 + 52000 = 542000
            assertThat(analytics.totalSalaryBill()).isEqualByComparingTo("542000");
        }

        @Test
        @DisplayName("returns ZERO when there are no employees")
        void emptyEmployees_returnsZero() {
            when(employeeService.findAll()).thenReturn(Collections.emptyList());

            assertThat(analytics.totalSalaryBill()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("single employee → total equals that employee's salary")
        void singleEmployee_totalEqualsSalary() {
            when(employeeService.findAll()).thenReturn(List.of(E3));

            assertThat(analytics.totalSalaryBill()).isEqualByComparingTo("95000");
        }
    }

    // =========================================================================
    // partitionByStatus()
    // =========================================================================

    @Nested
    @DisplayName("partitionByStatus()")
    class PartitionByStatusTests {

        private static final Employee INACTIVE_E1 =
                emp("I001", "D1", Role.ENGINEER, "45000", EmployeeStatus.INACTIVE);
        private static final Employee INACTIVE_E2 =
                emp("I002", "D2", Role.ANALYST,  "38000", EmployeeStatus.INACTIVE);

        @Test
        @DisplayName("partitions active and inactive employees correctly")
        void partitionsCorrectly() {
            when(employeeService.findAll()).thenReturn(
                    List.of(E1, E2, INACTIVE_E1, E3, INACTIVE_E2));

            Map<Boolean, List<Employee>> partitioned = analytics.partitionByStatus();

            assertThat(partitioned.get(true))
                    .as("active employees")
                    .containsExactlyInAnyOrder(E1, E2, E3);
            assertThat(partitioned.get(false))
                    .as("inactive employees")
                    .containsExactlyInAnyOrder(INACTIVE_E1, INACTIVE_E2);
        }

        @Test
        @DisplayName("map always has both keys even when one partition is empty")
        void allActive_inactivePartitionIsEmpty() {
            when(employeeService.findAll()).thenReturn(List.of(E1, E2));

            Map<Boolean, List<Employee>> partitioned = analytics.partitionByStatus();

            assertThat(partitioned).containsKeys(true, false);
            assertThat(partitioned.get(true)).hasSize(2);
            assertThat(partitioned.get(false)).isEmpty();
        }

        @Test
        @DisplayName("all inactive → active partition is empty")
        void allInactive_activePartitionIsEmpty() {
            when(employeeService.findAll()).thenReturn(List.of(INACTIVE_E1, INACTIVE_E2));

            Map<Boolean, List<Employee>> partitioned = analytics.partitionByStatus();

            assertThat(partitioned.get(true)).isEmpty();
            assertThat(partitioned.get(false)).hasSize(2);
        }

        @Test
        @DisplayName("empty employee list → both partitions are empty")
        void emptyEmployees_bothPartitionsEmpty() {
            when(employeeService.findAll()).thenReturn(Collections.emptyList());

            Map<Boolean, List<Employee>> partitioned = analytics.partitionByStatus();

            assertThat(partitioned).containsKeys(true, false);
            assertThat(partitioned.get(true)).isEmpty();
            assertThat(partitioned.get(false)).isEmpty();
        }

        @Test
        @DisplayName("partition lists are unmodifiable")
        void partitionLists_areUnmodifiable() {
            when(employeeService.findAll()).thenReturn(List.of(E1));

            Map<Boolean, List<Employee>> partitioned = analytics.partitionByStatus();

            assertThatThrownBy(() -> partitioned.get(true).add(E2))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> partitioned.get(false).add(E2))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("total count across both partitions equals findAll() size")
        void totalCount_matchesFindAll() {
            List<Employee> all = List.of(E1, E2, INACTIVE_E1, E3, INACTIVE_E2, E4);
            when(employeeService.findAll()).thenReturn(all);

            Map<Boolean, List<Employee>> partitioned = analytics.partitionByStatus();

            int total = partitioned.get(true).size() + partitioned.get(false).size();
            assertThat(total).isEqualTo(all.size());
        }
    }

    // =========================================================================
    // activeEmployees() / inactiveEmployees() convenience methods
    // =========================================================================

    @Nested
    @DisplayName("activeEmployees() / inactiveEmployees()")
    class ConvenienceMethodTests {

        private static final Employee INACTIVE_E1 =
                emp("I001", "D1", Role.HR, "42000", EmployeeStatus.INACTIVE);

        @Test
        @DisplayName("activeEmployees() returns only ACTIVE employees")
        void activeEmployees_returnsOnlyActive() {
            when(employeeService.findAll()).thenReturn(List.of(E1, INACTIVE_E1, E2));

            List<Employee> active = analytics.activeEmployees();

            assertThat(active).containsExactlyInAnyOrder(E1, E2);
        }

        @Test
        @DisplayName("inactiveEmployees() returns only INACTIVE employees")
        void inactiveEmployees_returnsOnlyInactive() {
            when(employeeService.findAll()).thenReturn(List.of(E1, INACTIVE_E1, E2));

            List<Employee> inactive = analytics.inactiveEmployees();

            assertThat(inactive).containsExactly(INACTIVE_E1);
        }

        @Test
        @DisplayName("activeEmployees() returns empty list when all are inactive")
        void activeEmployees_emptyWhenAllInactive() {
            when(employeeService.findAll()).thenReturn(List.of(INACTIVE_E1));

            assertThat(analytics.activeEmployees()).isEmpty();
        }

        @Test
        @DisplayName("inactiveEmployees() returns empty list when all are active")
        void inactiveEmployees_emptyWhenAllActive() {
            when(employeeService.findAll()).thenReturn(List.of(E1, E2));

            assertThat(analytics.inactiveEmployees()).isEmpty();
        }
    }

    // =========================================================================
    // Cross-method invariants
    // =========================================================================

    @Nested
    @DisplayName("Cross-method invariants")
    class InvariantTests {

        @Test
        @DisplayName("maxSalary >= minSalary when employees exist")
        void maxGreaterThanOrEqualToMin() {
            when(employeeService.findAll()).thenReturn(ALL);

            assertThat(analytics.maxSalary())
                    .isGreaterThanOrEqualTo(analytics.minSalary());
        }

        @Test
        @DisplayName("totalSalaryBill >= maxSalary when employees exist")
        void totalGreaterThanOrEqualToMax() {
            when(employeeService.findAll()).thenReturn(ALL);

            assertThat(analytics.totalSalaryBill())
                    .isGreaterThanOrEqualTo(analytics.maxSalary());
        }

        @Test
        @DisplayName("groupByDepartment contains every employee exactly once")
        void groupByDepartment_totalCountMatchesFindAll() {
            when(employeeService.findAll()).thenReturn(ALL);

            long totalGrouped = analytics.groupByDepartment().values().stream()
                    .mapToLong(List::size)
                    .sum();

            assertThat(totalGrouped).isEqualTo(ALL.size());
        }

        @Test
        @DisplayName("top5 first element salary equals maxSalary")
        void top5FirstElement_equalsMaxSalary() {
            when(employeeService.findAll()).thenReturn(ALL);

            BigDecimal top1Salary = analytics.top5HighestSalaries().get(0).getSalary();

            assertThat(top1Salary).isEqualByComparingTo(analytics.maxSalary());
        }
    }
}

