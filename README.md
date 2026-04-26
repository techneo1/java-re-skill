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
│   ├── App.java                             # Entry point – wires all layers
│   ├── config/
│   │   ├── AppConfig.java                   # Application configuration holder
│   │   └── ConfigLoader.java                # Loads properties from app.properties
│   ├── domain/                              # ① DOMAIN LAYER
│   │   ├── Employee.java                    # Employee entity (immutable, Builder)
│   │   ├── Department.java                  # Department entity (immutable, Builder)
│   │   ├── PayrollRecord.java               # Payroll record entity
│   │   ├── EmployeeId.java                  # Value object – typed employee key
│   │   └── enums/
│   │       ├── Role.java                    # Employee roles
│   │       └── EmployeeStatus.java          # ACTIVE / INACTIVE / …
│   ├── exception/                           # Custom exception hierarchy
│   │   ├── HrException.java                 # Abstract root (unchecked)
│   │   ├── BusinessRuleException.java
│   │   ├── ResourceNotFoundException.java
│   │   ├── ConfigurationException.java
│   │   ├── EmployeeNotFoundException.java
│   │   ├── DepartmentNotFoundException.java
│   │   ├── DuplicateEmailException.java
│   │   └── InvalidSalaryException.java
│   ├── repository/                          # ② REPOSITORY LAYER
│   │   ├── GenericRepository.java           # Generic CRUD interface <T, ID>
│   │   ├── EmployeeRepository.java          # Employee-specific queries
│   │   ├── DepartmentRepository.java        # Department-specific queries
│   │   └── inmemory/                        # In-memory implementations
│   │       ├── InMemoryRepository.java      # Abstract base (HashMap + ArrayList)
│   │       ├── InMemoryEmployeeRepository.java
│   │       └── InMemoryDepartmentRepository.java
│   └── service/                             # ③ SERVICE LAYER
│       ├── EmployeeService.java             # Employee business-logic contract
│       ├── DepartmentService.java           # Department business-logic contract
│       └── impl/
│           ├── EmployeeServiceImpl.java     # Validates rules, delegates to repo
│           └── DepartmentServiceImpl.java
└── main/resources/
    └── app.properties
```

---

## Layered Architecture

```
┌──────────────────────────────────────┐
│           service layer              │  Business rules (e.g. duplicate e-mail)
│  EmployeeService / DepartmentService │  Delegates writes/reads to repositories
└────────────────┬─────────────────────┘
                 │
┌────────────────▼─────────────────────┐
│         repository layer             │  GenericRepository<T, ID> interface
│  InMemoryEmployee/DepartmentRepo     │  inmemory/ sub-package implementations
└────────────────┬─────────────────────┘
                 │
┌────────────────▼─────────────────────┐
│           domain layer               │  Entities, value objects, enums
│  Employee · Department · EmployeeId  │  Pure Java, no framework dependencies
└──────────────────────────────────────┘
```

---

## Key Concepts Covered

- **Domain modelling** – entities, value objects (`EmployeeId`), enums
- **Custom exception hierarchy** – unchecked, polymorphic catch
- **Generics** – `GenericRepository<T, ID>` with strategy-based in-memory base
- **Collections & Streams** – in-memory CRUD, filtering, sorting
- **Service layer** – business-rule enforcement (duplicate e-mail, not-found)
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
