# JDBC DAO Layer Implementation

## Overview
This document describes the JDBC Data Access Object (DAO) layer implementation using PreparedStatement for the java-re-skill project.

## Architecture

```
Domain Objects (Employee, Department)
     ↓
Repository Interfaces (EmployeeRepository, DepartmentRepository)
     ↓
JDBC DAO Implementations (JdbcEmployeeRepository, JdbcDepartmentRepository)
     ↓
ConnectionManager (HikariCP Connection Pooling)
     ↓
H2 In-Memory Database
```

## Components Implemented

### 1. ConnectionManager.java
**Location**: `src/main/java/com/srikanth/javareskill/repository/jdbc/ConnectionManager.java`

**Purpose**: Centralized database connection management using HikariCP connection pooling.

**Key Features**:
- Singleton pattern with explicit lifecycle management
- HikariCP connection pooling for optimal performance
- Configurable connection pool settings (max pool size: 10, min idle: 2)
- Support for custom JDBC URL or default H2 in-memory database
- Thread-safe initialization and shutdown

**Usage**:
```java
// Initialize connection pool
ConnectionManager.initializeDefault();

// Get a connection (use try-with-resources)
try (Connection conn = ConnectionManager.getConnection()) {
    // Use connection
}

// Shutdown on application exit
ConnectionManager.shutdown();
```

### 2. SchemaInitializer.java
**Location**: `src/main/java/com/srikanth/javareskill/repository/jdbc/SchemaInitializer.java`

**Purpose**: Database schema initialization utility.

**Tables Created**:
- `departments` (id, name, location)
- `employees` (id, name, email, department_id, role, salary, status, joining_date)

**Indexes**:
- `idx_emp_dept` on employees(department_id)
- `idx_emp_status` on employees(status)
- `idx_emp_role` on employees(role)
- `idx_dept_location` on departments(location)

**Usage**:
```java
SchemaInitializer.createTables();  // Create schema
SchemaInitializer.dropTables();    // Drop tables (for testing)
```

### 3. JdbcDepartmentRepository.java
**Location**: `src/main/java/com/srikanth/javareskill/repository/jdbc/JdbcDepartmentRepository.java`

**Purpose**: JDBC implementation of DepartmentRepository using PreparedStatement.

**Key Features**:
- All SQL queries use PreparedStatement for SQL injection prevention
- try-with-resources for automatic resource cleanup
- Proper exception handling and error messages
- Immutable result lists using `List.copyOf()`

**Methods Implemented**:
- `save(Department)` - INSERT with duplicate key detection
- `findById(String)` - SELECT by primary key
- `findAll()` - SELECT all departments
- `update(Department)` - UPDATE with existence check
- `deleteById(String)` - DELETE with existence check
- `existsById(String)` - Check existence
- `count()` - Count all departments
- `findByLocation(String)` - Query by location (case-insensitive)
- `findByName(String)` - Query by name (case-insensitive)

### 4. JdbcEmployeeRepository.java
**Location**: `src/main/java/com/srikanth/javareskill/repository/jdbc/JdbcEmployeeRepository.java`

**Purpose**: JDBC implementation of EmployeeRepository using PreparedStatement.

**Key Features**:
- PreparedStatement for all queries
- Proper enum mapping (Role, EmployeeStatus)
- BigDecimal handling for salary precision
- LocalDate to SQL Date conversion
- Comprehensive CRUD operations

**Methods Implemented**:
- `save(Employee)` - INSERT employee
- `findById(EmployeeId)` - SELECT by ID
- `findAll()` - SELECT all employees
- `update(Employee)` - UPDATE employee
- `deleteById(EmployeeId)` - DELETE employee
- `existsById(EmployeeId)` - Check existence
- `count()` - Count employees
- `findByDepartmentId(String)` - Query by department
- `findByStatus(EmployeeStatus)` - Query by status
- `findByRole(Role)` - Query by role

**Helper Methods**:
- `setEmployeeParameters()` - Binds employee fields to PreparedStatement
- `mapRowToEmployee()` - Maps ResultSet row to Employee domain object

## JDBC Best Practices Demonstrated

### 1. PreparedStatement Usage
All queries use parameterized statements:
```java
private static final String SELECT_BY_ID_SQL = """
    SELECT id, name, email, department_id, role, salary, status, joining_date
    FROM employees WHERE id = ?
""";

pstmt.setString(1, id.getValue());
```

**Benefits**:
- Prevents SQL injection attacks
- Improved performance through query plan caching
- Type-safe parameter binding

### 2. try-with-resources
Automatic resource management:
```java
try (Connection conn = ConnectionManager.getConnection();
     PreparedStatement pstmt = conn.prepareStatement(sql);
     ResultSet rs = pstmt.executeQuery()) {
    // Use resources
} // Automatically closed
```

### 3. Exception Handling
- SQL exceptions are caught and wrapped with context
- Duplicate key violations are detected and converted to IllegalArgumentException
- NotFoundException thrown when entity doesn't exist

### 4. Type Mapping
- **Enums**: Stored as VARCHAR, converted using `Enum.valueOf()`
- **BigDecimal**: Mapped to DECIMAL(15, 2) for salary precision
- **LocalDate**: Converted to SQL Date using `Date.valueOf()`

### 5. Result Set Immutability
All list results are wrapped in `List.copyOf()` to return immutable collections.

## Dependencies Added

### pom.xml additions:
```xml
<!-- H2 Database (embedded JDBC database) -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.2.224</version>
</dependency>

<!-- HikariCP (connection pooling) -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

## Demo Application

### JdbcDemoApp.java
**Location**: `src/main/java/com/srikanth/javareskill/JdbcDemoApp.java`

Demonstrates complete JDBC workflow:
1. Initialize connection pool
2. Create database schema
3. Wire up JDBC repositories
4. Perform CRUD operations
5. Execute queries
6. Run salary analytics
7. Clean up resources

**Run the demo**:
```bash
mvn exec:java -Dexec.mainClass="com.srikanth.javareskill.JdbcDemoApp"
```

## Integration Tests

### JdbcRepositoryIntegrationTest.java
**Location**: `src/test/java/com/srikanth/javareskill/repository/jdbc/JdbcRepositoryIntegrationTest.java`

Comprehensive integration tests covering:
- Department CRUD operations
- Employee CRUD operations
- Query operations (by department, status, role)
- Edge cases and validation

**Run tests**:
```bash
mvn test -Dtest=JdbcRepositoryIntegrationTest
```

## SOLID Principles Applied

### Single Responsibility Principle (SRP)
- **ConnectionManager**: Only manages database connections
- **SchemaInitializer**: Only handles schema DDL
- **JdbcEmployeeRepository**: Only handles employee persistence
- **JdbcDepartmentRepository**: Only handles department persistence

### Open/Closed Principle (OCP)
- Repository implementations can be extended without modifying interfaces
- New query methods can be added to implementations

### Liskov Substitution Principle (LSP)
- JDBC repositories are fully substitutable for in-memory implementations
- All repository methods honor the contracts defined in interfaces

### Interface Segregation Principle (ISP)
- GenericRepository provides basic CRUD operations
- Domain-specific repositories extend with specialized queries
- Clients depend only on methods they need

### Dependency Inversion Principle (DIP)
- Services depend on repository interfaces, not concrete implementations
- Easy to swap in-memory for JDBC or vice versa
- Connection management is abstracted behind ConnectionManager

## Performance Considerations

### Connection Pooling
- HikariCP provides high-performance connection pooling
- Pool size: 10 max connections, 2 minimum idle
- Connection timeout: 30 seconds
- Idle timeout: 10 minutes
- Max lifetime: 30 minutes

### Query Optimization
- Indexes created on frequently queried columns
- PreparedStatement provides query plan caching
- Batch operations possible for bulk inserts/updates

### Resource Management
- All connections auto-closed via try-with-resources
- Proper cleanup in finally blocks
- Connection pool shutdown on application exit

## Security

### SQL Injection Prevention
All queries use parameterized PreparedStatement:
```java
// SECURE: Parameters are bound, not concatenated
pstmt.setString(1, email);

// INSECURE (NOT USED): String concatenation
// "SELECT * FROM employees WHERE email = '" + email + "'"
```

### Data Validation
- Null checks on all method parameters
- Domain validation in entity constructors
- Foreign key constraints enforced at database level

## Future Enhancements

Potential improvements:
1. **Transaction Management**: Implement transaction boundaries for multi-table operations
2. **Batch Operations**: Add batch insert/update methods for bulk data
3. **Pagination**: Implement LIMIT/OFFSET for large result sets
4. **Audit Logging**: Track creation/modification timestamps
5. **Connection Pool Monitoring**: JMX metrics for pool health
6. **Database Migration**: Use Flyway or Liquibase for schema versioning
7. **Read/Write Splitting**: Separate connections for read-only vs write operations

## Conclusion

The JDBC DAO layer provides a robust, secure, and performant persistence mechanism that:
- Follows SOLID principles
- Demonstrates JDBC best practices
- Prevents SQL injection
- Manages resources efficiently
- Integrates seamlessly with existing architecture
- Can be easily swapped with in-memory implementations for testing

