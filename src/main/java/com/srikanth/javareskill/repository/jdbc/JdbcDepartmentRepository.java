package com.srikanth.javareskill.repository.jdbc;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.exception.DepartmentNotFoundException;
import com.srikanth.javareskill.repository.DepartmentRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC implementation of {@link DepartmentRepository} using {@link PreparedStatement}.
 *
 * <h2>JDBC best practices demonstrated</h2>
 * <ul>
 *   <li><b>PreparedStatement</b> – all queries use parameterized statements to
 *       prevent SQL injection and improve performance through query plan caching.</li>
 *   <li><b>try-with-resources</b> – ensures automatic closure of
 *       {@code Connection}, {@code PreparedStatement}, and {@code ResultSet}.</li>
 *   <li><b>Consistent exception handling</b> – wraps {@link SQLException} in
 *       domain-specific exceptions where appropriate.</li>
 * </ul>
 *
 * <h2>SOLID principles applied</h2>
 * <ul>
 *   <li><b>S – Single Responsibility</b>: This class handles only department
 *       persistence — business logic stays in service classes.</li>
 *   <li><b>D – Dependency Inversion</b>: Implements {@link DepartmentRepository}
 *       abstraction; services depend on the interface, not this class.</li>
 * </ul>
 */
public class JdbcDepartmentRepository implements DepartmentRepository {

    // -------------------------------------------------------------------------
    // SQL statements (constants for clarity and maintainability)
    // -------------------------------------------------------------------------

    private static final String INSERT_SQL = """
        INSERT INTO departments (id, name, location)
        VALUES (?, ?, ?)
    """;

    private static final String SELECT_BY_ID_SQL = """
        SELECT id, name, location
        FROM departments
        WHERE id = ?
    """;

    private static final String SELECT_ALL_SQL = """
        SELECT id, name, location
        FROM departments
        ORDER BY id
    """;

    private static final String UPDATE_SQL = """
        UPDATE departments
        SET name = ?, location = ?
        WHERE id = ?
    """;

    private static final String DELETE_SQL = """
        DELETE FROM departments
        WHERE id = ?
    """;

    private static final String EXISTS_SQL = """
        SELECT 1
        FROM departments
        WHERE id = ?
    """;

    private static final String COUNT_SQL = """
        SELECT COUNT(*)
        FROM departments
    """;

    private static final String SELECT_BY_LOCATION_SQL = """
        SELECT id, name, location
        FROM departments
        WHERE LOWER(location) = LOWER(?)
        ORDER BY id
    """;

    private static final String SELECT_BY_NAME_SQL = """
        SELECT id, name, location
        FROM departments
        WHERE LOWER(name) = LOWER(?)
    """;

    // -------------------------------------------------------------------------
    // GenericRepository implementation
    // -------------------------------------------------------------------------

    @Override
    public void save(Department department) {
        Objects.requireNonNull(department, "department must not be null");

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {

            pstmt.setString(1, department.getId());
            pstmt.setString(2, department.getName());
            pstmt.setString(3, department.getLocation());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalStateException("Failed to insert department: " + department.getId());
            }

        } catch (SQLException e) {
            // H2 will throw SQLException with error code 23505 for duplicate primary key
            if (e.getErrorCode() == 23505 || e.getMessage().contains("Unique index or primary key violation")) {
                throw new IllegalArgumentException("Department with id '" + department.getId() + "' already exists", e);
            }
            throw new RuntimeException("Failed to save department: " + department.getId(), e);
        }
    }

    @Override
    public Optional<Department> findById(String id) {
        Objects.requireNonNull(id, "id must not be null");

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToDepartment(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find department by id: " + id, e);
        }
    }

    @Override
    public List<Department> findAll() {
        List<Department> departments = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                departments.add(mapRowToDepartment(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve all departments", e);
        }

        return List.copyOf(departments);  // Immutable list
    }

    @Override
    public void update(Department department) {
        Objects.requireNonNull(department, "department must not be null");

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {

            pstmt.setString(1, department.getName());
            pstmt.setString(2, department.getLocation());
            pstmt.setString(3, department.getId());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DepartmentNotFoundException(department.getId());
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update department: " + department.getId(), e);
        }
    }

    @Override
    public void deleteById(String id) {
        Objects.requireNonNull(id, "id must not be null");

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_SQL)) {

            pstmt.setString(1, id);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DepartmentNotFoundException(id);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete department: " + id, e);
        }
    }

    @Override
    public boolean existsById(String id) {
        Objects.requireNonNull(id, "id must not be null");

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(EXISTS_SQL)) {

            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check existence of department: " + id, e);
        }
    }

    @Override
    public int count() {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(COUNT_SQL);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to count departments", e);
        }
    }

    // -------------------------------------------------------------------------
    // DepartmentRepository-specific methods
    // -------------------------------------------------------------------------

    @Override
    public List<Department> findByLocation(String location) {
        Objects.requireNonNull(location, "location must not be null");

        List<Department> departments = new ArrayList<>();

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_LOCATION_SQL)) {

            pstmt.setString(1, location);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    departments.add(mapRowToDepartment(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find departments by location: " + location, e);
        }

        return List.copyOf(departments);
    }

    @Override
    public Optional<Department> findByName(String name) {
        Objects.requireNonNull(name, "name must not be null");

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_NAME_SQL)) {

            pstmt.setString(1, name);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToDepartment(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find department by name: " + name, e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link ResultSet} row to a {@link Department} domain object.
     *
     * <p>The cursor must be positioned on a valid row before calling this method.</p>
     *
     * @param rs the result set
     * @return a new {@link Department} instance
     * @throws SQLException if column access fails
     */
    private Department mapRowToDepartment(ResultSet rs) throws SQLException {
        return Department.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .location(rs.getString("location"))
                .build();
    }
}

