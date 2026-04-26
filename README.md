# java-re-skill

A hands-on Java re-skilling project covering core Java concepts through a small HR / Payroll domain application.

---

## Tech Stack

| Tool | Version |
|------|---------|
| Java | 17 |
| Maven | 3.x |
| JUnit 5 | 5.10.2 |
| AssertJ | 3.25.3 |

---

## Project Structure

```
src/
├── main/java/com/srikanth/javareskill/
│   ├── App.java                        # Entry point
│   ├── config/
│   │   ├── AppConfig.java              # Application configuration holder
│   │   └── ConfigLoader.java           # Loads properties from app.properties
│   ├── domain/
│   │   ├── Employee.java               # Employee entity
│   │   ├── Department.java             # Department entity
│   │   ├── PayrollRecord.java          # Payroll record entity
│   │   └── enums/
│   │       ├── Role.java               # Employee roles
│   │       └── EmployeeStatus.java     # Active / Inactive / On-leave, etc.
│   ├── exception/
│   │   ├── HrException.java            # Base HR exception
│   │   ├── BusinessRuleException.java  # Business rule violations
│   │   ├── ResourceNotFoundException.java
│   │   ├── ConfigurationException.java
│   │   ├── EmployeeNotFoundException.java
│   │   ├── DepartmentNotFoundException.java
│   │   ├── DuplicateEmailException.java
│   │   └── InvalidSalaryException.java
│   ├── repository/
│   │   ├── GenericRepository.java           # Generic CRUD interface
│   │   ├── InMemoryRepository.java          # Generic in-memory implementation
│   │   ├── EmployeeRepository.java          # Employee-specific queries
│   │   ├── InMemoryEmployeeRepository.java
│   │   ├── DepartmentRepository.java        # Department-specific queries
│   │   └── InMemoryDepartmentRepository.java
│   └── store/
│       ├── EmployeeId.java             # Value object wrapping employee ID
│       ├── EmployeeStore.java          # Store interface
│       └── InMemoryEmployeeStore.java  # In-memory store implementation
└── main/resources/
    └── app.properties                  # Application properties
```

---

## Key Concepts Covered

- **Domain modelling** – entities, value objects, enums
- **Custom exception hierarchy** – checked / unchecked, polymorphic catch
- **Generics** – `GenericRepository<T, ID>` with bounded type parameters
- **Collections & Streams** – in-memory CRUD, filtering, sorting
- **Configuration loading** – reading `.properties` files at runtime
- **Unit testing** – JUnit 5 nested tests, AssertJ fluent assertions

---

## Getting Started

### Prerequisites

- JDK 17+
- Maven 3.6+

### Build

```bash
mvn clean compile
```

### Run Tests

```bash
mvn test
```

### Package

```bash
mvn package
```

The resulting JAR will be at `target/java-re-skill-1.0.0-SNAPSHOT.jar`.

---

## Configuration

Application settings are managed via `src/main/resources/app.properties`.  
`ConfigLoader` reads these properties at startup and exposes them through `AppConfig`.

---

## License

See [LICENSE](LICENSE).
