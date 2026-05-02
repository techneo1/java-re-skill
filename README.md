# java-re-skill

A hands-on Java re-skilling project covering core Java concepts through a small HR / Payroll domain application.

---

## Tech Stack

| Tool | Version |
|------|---------|
| Java | 17 |
| Maven | 3.x |
| Spring Boot | 3.2.5 |
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
│   ├── service/                             # ③ SERVICE LAYER
│   │   ├── EmployeeService.java             # Employee business-logic contract
│   │   ├── DepartmentService.java           # Department business-logic contract
│   │   ├── ValidationService.java           # Validation contract (email, salary, dept)
│   │   └── impl/
│   │       ├── EmployeeServiceImpl.java     # Validates rules, delegates to repo
│   │       ├── DepartmentServiceImpl.java
│   │       └── ValidationServiceImpl.java   # Concrete validation logic
│   └── payroll/                             # ④ PAYROLL LAYER
│       ├── PayrollService.java              # Payroll processing contract
│       ├── impl/
│       │   └── PayrollServiceImpl.java
│       └── strategy/
│           ├── TaxStrategy.java             # Strategy interface
│           ├── TaxStrategyFactory.java      # Role → strategy registry (extensible)
│           ├── FlatRateTaxStrategy.java
│           ├── ProgressiveTaxStrategy.java
│           └── ExemptTaxStrategy.java
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

## SOLID Principles

The entire codebase is designed around the SOLID principles. The table below maps
each principle to concrete locations in the source tree.

### S — Single Responsibility Principle

> *A class should have only one reason to change.*

| Where | What |
|-------|------|
| `Employee.java` | Represents only employee state — no persistence, no validation. |
| `ValidationService` / `ValidationServiceImpl` | All validation rules (email uniqueness, salary range, department existence) live here and nowhere else. `EmployeeServiceImpl.hire()` **delegates** to `ValidationService` instead of duplicating the check inline. |
| `PayrollServiceImpl` | Responsible only for payroll record generation; delegates every tax calculation to an injected `TaxStrategy`. |
| `ConfigLoader` | Solely responsible for reading `.properties` files; `AppConfig` holds the resulting values. |
| Each exception class | Every concrete exception (`EmployeeNotFoundException`, `DuplicateEmailException`, …) encodes exactly one failure scenario. Shared fields are factored into the `HrException` base. |

### O — Open/Closed Principle

> *Software entities should be open for extension, but closed for modification.*

| Where | What |
|-------|------|
| `TaxStrategy` interface | Adding a new tax rule (e.g. `SurchargeStrategy`) requires only implementing `TaxStrategy` — `PayrollServiceImpl` and `PayrollService` are never touched. |
| `TaxStrategyFactory.register(Role, TaxStrategy)` | The built-in role→strategy defaults are *closed for modification*, but `register()` allows callers to add or override mappings at runtime *without editing the factory source*. |
| `InMemoryRepository<T, ID>` | The CRUD engine is closed — subclasses extend it by adding domain-specific queries (`findByDepartmentId`, `findByLocation`) without modifying `InMemoryRepository`. |
| `HrException` hierarchy | New exception categories are added by subclassing `HrException` / `ResourceNotFoundException` / `BusinessRuleException` — the base classes are never changed. |

### L — Liskov Substitution Principle

> *Objects of a subtype must be substitutable for objects of their supertype.*

| Where | What |
|-------|------|
| `InMemoryEmployeeRepository` / `InMemoryDepartmentRepository` | Both extend `InMemoryRepository<T, ID>` and implement their respective domain interfaces. Either can be substituted for `GenericRepository<T, ID>` anywhere. |
| `FlatRateTaxStrategy`, `ProgressiveTaxStrategy`, `ExemptTaxStrategy` | All honour the `TaxStrategy` contract (non-null, non-negative result ≤ grossSalary). They are fully interchangeable wherever `TaxStrategy` is expected — demonstrated by the `strategyIsSwappable` test. |
| `EmployeeNotFoundException` / `DepartmentNotFoundException` | Both are caught interchangeably as `ResourceNotFoundException` or as `HrException`; the polymorphic catch tests in `HrExceptionHierarchyTest` verify this. |

### I — Interface Segregation Principle

> *Clients should not be forced to depend on methods they do not use.*

| Where | What |
|-------|------|
| `GenericRepository<T, ID>` | Declares only generic CRUD operations. Domain-specific queries are added in narrower sub-interfaces (`EmployeeRepository`, `DepartmentRepository`). A component that only needs `findAll()` and `save()` can depend on `GenericRepository` without being coupled to HR-specific methods. |
| `EmployeeRepository` / `DepartmentRepository` | Each extends `GenericRepository` and adds only the finders relevant to that aggregate (e.g. `findByDepartmentId`, `findByLocation`). |
| `ValidationService` | Focused exclusively on validation. Service classes that have no validation concern are not forced to depend on this interface. |
| `TaxStrategy` | A minimal two-method contract (`calculateTax`, `name`). Implementations are not burdened with unrelated concerns. |

### D — Dependency Inversion Principle

> *High-level modules should not depend on low-level modules; both should depend on abstractions.*

| Where | What |
|-------|------|
| `EmployeeServiceImpl(EmployeeRepository, ValidationService)` | Depends on two *interfaces*, not on `InMemoryEmployeeRepository` or `ValidationServiceImpl`. The concrete implementations are injected by the caller (`App.java`). |
| `PayrollServiceImpl.process(Employee, LocalDate, TaxStrategy)` | Accepts the `TaxStrategy` abstraction; the caller decides which concrete strategy to pass. |
| `App.java` wiring | The entry point is the only place where concrete classes appear in `new` expressions. All service variables are declared as interface types. |
| All repository usages | Every service depends on `EmployeeRepository` / `DepartmentRepository` (interfaces), never on `InMemory*` classes directly. |

---

## Key Concepts Covered

- **Domain modelling** – entities, value objects (`EmployeeId`), enums
- **Custom exception hierarchy** – unchecked, polymorphic catch
- **Generics** – `GenericRepository<T, ID>` with strategy-based in-memory base
- **Collections & Streams** – in-memory CRUD, filtering, sorting
- **Service layer** – business-rule enforcement (duplicate e-mail, not-found)
- **Configuration loading** – reading `.properties` files at runtime
- **Unit testing** – JUnit 5 nested tests, AssertJ fluent assertions
- **SOLID principles** – applied and documented throughout

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

## Running the Application

### Default (dev profile)

```bash
# Maven plugin — activates the 'dev' profile automatically
mvn spring-boot:run
```

### Explicit profile selection

```bash
# Dev profile (verbose logging, H2 console at http://localhost:8080/h2-console)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Test profile (random port, minimal logging, no H2 console)
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

### Fat-jar (after `mvn package`)

```bash
# Dev
java -jar target/java-re-skill-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev

# Test
java -jar target/java-re-skill-1.0.0-SNAPSHOT.jar --spring.profiles.active=test
```

### IDE run configuration

Add the following VM argument to your run configuration:

```
-Dspring.profiles.active=dev
```

---

## Spring Profiles

| Setting | `dev` | `test` |
|---|---|---|
| Server port | `8080` | `0` (random) |
| Stack-trace in errors | ✅ always | ❌ never |
| H2 web console | ✅ `/h2-console` | ❌ disabled |
| DevTools auto-restart | ✅ enabled | ❌ disabled |
| H2 database name | `hrdb-dev` | `hrdb-test` |
| App log level | `DEBUG` | `INFO` |
| Spring log level | `INFO` | `WARN` |
| `app.max-employees` | `1000` | `50` |
| `app.audit.enabled` | `true` | `false` |

Profile-specific files:

```
src/main/resources/
├── application.properties          ← shared base (Jackson, app name, default profile)
├── application-dev.properties      ← dev overrides
└── application-test.properties     ← test overrides (main classpath)

src/test/resources/
└── application-test.properties     ← test overrides (test classpath / @ActiveProfiles)
```

---

## Configuration

Application settings are managed via Spring Boot profile properties files.  
`AppProperties` (`@ConfigurationProperties(prefix = "app")`) binds the `app.*` keys
as a typed bean injectable anywhere in the application.

| Property | dev default | test default |
|---|---|---|
| `app.env` | `dev` | `test` |
| `app.max-employees` | `1000` | `50` |
| `app.default-salary` | `50000.00` | `30000.00` |
| `app.audit.enabled` | `true` | `false` |

---

## License

See [LICENSE](LICENSE).
