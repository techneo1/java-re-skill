package com.srikanth.javareskill.repository.jdbc;

import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.EmployeeId;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.exception.EmployeeNotFoundException;
import com.srikanth.javareskill.repository.EmployeeRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC implementation of {@link EmployeeRepository} using {@link PreparedStatement}.
 */
public class JdbcEmployeeRepository implements EmployeeRepository {

    private static final String INSERT_SQL = """
        INSERT INTO employees (id, name, email, department_id, role, salary, status, joining_date)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """;

    private static final String SELECT_BY_ID_SQL = """
        SELECT id, name, email, department_id, role, salary, status, joining_date
        FROM employees WHERE id = ?
    """;

    private static final String SELECT_ALL_SQL = """
        SELECT id, name, email, department_id, role, salary, status, joining_date
        FROM employees ORDER BY id
    """;

    private static final String UPDATE_SQL = """
        UPDATE employees SET name = ?, email = ?, department_id = ?, role = ?, salary = ?, status = ?, joining_date = ?
        WHERE id = ?
    """;

    private static final String DELETE_SQL = "DELETE FROM employees WHERE id = ?";
    private static final String EXISTS_SQL = "SELECT 1 FROM employees WHERE id = ?";
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM employees";

    private static final String SELECT_BY_DEPARTMENT_SQL = """
        SELECT id, name, email, department_id, role, salary, status, joining_date
        FROM employees WHERE department_id = ? ORDER BY id
    """;

    private static final String SELECT_BY_STATUS_SQL = """
        SELECT id, name, email, department_id, role, salary, status, joining_date
        FROM employees WHERE status = ? ORDER BY id
    """;

    private static final String SELECT_BY_ROLE_SQL = """
        SELECT id, name, email, department_id, role, salary, status, joining_date
        FROM employees WHERE role = ? ORDER BY id
    """;

    @Override
    public void save(Employee employee) {
        Objects.requireNonNull(employee, "employee must not be null");
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
            setEmployeeParameters(pstmt, employee);
            int rows = pstmt.executeUpdate();
            if (rows == 0) throw new IllegalStateException("Failed to insert employee: " + employee.getId());
        } catch (SQLException e) {
            if (e.getErrorCode() == 23505 || e.getMessage().contains("Unique index or primary key violation")) {
                throw new IllegalArgumentException("Employee with id '" + employee.getId() + "' already exists", e);
            }
            throw new RuntimeException("Failed to save employee: " + employee.getId(), e);
        }
    }

    @Override
    public Optional<Employee> findById(EmployeeId id) {
        Objects.requireNonNull(id, "id must not be null");
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {
            pstmt.setString(1, id.getValue());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRowToEmployee(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find employee by id: " + id, e);
        }
    }

    @Override
    public List<Employee> findAll() {
        List<Employee> employees = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) employees.add(mapRowToEmployee(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve all employees", e);
        }
        return List.copyOf(employees);
    }

    @Override
    public void update(Employee employee) {
        Objects.requireNonNull(employee, "employee must not be null");
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {
            pstmt.setString(1, employee.getName());
            pstmt.setString(2, employee.getEmail());
            pstmt.setString(3, employee.getDepartmentId());
            pstmt.setString(4, employee.getRole().name());
            pstmt.setBigDecimal(5, employee.getSalary());
            pstmt.setString(6, employee.getStatus().name());
            pstmt.setDate(7, Date.valueOf(employee.getJoiningDate()));
            pstmt.setString(8, employee.getId());
            int rows = pstmt.executeUpdate();
            if (rows == 0) throw new EmployeeNotFoundException(employee.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update employee: " + employee.getId(), e);
        }
    }

    @Override
    public void deleteById(EmployeeId id) {
        Objects.requireNonNull(id, "id must not be null");
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_SQL)) {
            pstmt.setString(1, id.getValue());
            int rows = pstmt.executeUpdate();
            if (rows == 0) throw new EmployeeNotFoundException(id.getValue());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete employee: " + id, e);
        }
    }

    @Override
    public boolean existsById(EmployeeId id) {
        Objects.requireNonNull(id, "id must not be null");
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(EXISTS_SQL)) {
            pstmt.setString(1, id.getValue());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check existence of employee: " + id, e);
        }
    }

    @Override
    public int count() {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(COUNT_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count employees", e);
        }
    }

    @Override
    public List<Employee> findByDepartmentId(String departmentId) {
        Objects.requireNonNull(departmentId, "departmentId must not be null");
        List<Employee> employees = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_DEPARTMENT_SQL)) {
            pstmt.setString(1, departmentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) employees.add(mapRowToEmployee(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find employees by department: " + departmentId, e);
        }
        return List.copyOf(employees);
    }

    @Override
    public List<Employee> findByStatus(EmployeeStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        List<Employee> employees = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_STATUS_SQL)) {
            pstmt.setString(1, status.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) employees.add(mapRowToEmployee(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find employees by status: " + status, e);
        }
        return List.copyOf(employees);
    }

    @Override
    public List<Employee> findByRole(Role role) {
        Objects.requireNonNull(role, "role must not be null");
        List<Employee> employees = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_ROLE_SQL)) {
            pstmt.setString(1, role.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) employees.add(mapRowToEmployee(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find employees by role: " + role, e);
        }
        return List.copyOf(employees);
    }

    private void setEmployeeParameters(PreparedStatement pstmt, Employee employee) throws SQLException {
        pstmt.setString(1, employee.getId());
        pstmt.setString(2, employee.getName());
        pstmt.setString(3, employee.getEmail());
        pstmt.setString(4, employee.getDepartmentId());
        pstmt.setString(5, employee.getRole().name());
        pstmt.setBigDecimal(6, employee.getSalary());
        pstmt.setString(7, employee.getStatus().name());
        pstmt.setDate(8, Date.valueOf(employee.getJoiningDate()));
    }

    private Employee mapRowToEmployee(ResultSet rs) throws SQLException {
        return Employee.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .email(rs.getString("email"))
                .departmentId(rs.getString("department_id"))
                .role(Role.valueOf(rs.getString("role")))
                .salary(rs.getBigDecimal("salary"))
                .status(EmployeeStatus.valueOf(rs.getString("status")))
                .joiningDate(rs.getDate("joining_date").toLocalDate())
                .build();
    }
}

