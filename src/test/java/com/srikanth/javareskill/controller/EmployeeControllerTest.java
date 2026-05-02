package com.srikanth.javareskill.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srikanth.javareskill.domain.Employee;
import com.srikanth.javareskill.domain.EmployeeId;
import com.srikanth.javareskill.domain.enums.EmployeeStatus;
import com.srikanth.javareskill.domain.enums.Role;
import com.srikanth.javareskill.dto.request.CreateEmployeeRequest;
import com.srikanth.javareskill.dto.request.UpdateEmployeeStatusRequest;
import com.srikanth.javareskill.exception.EmployeeNotFoundException;
import com.srikanth.javareskill.exception.GlobalExceptionHandler;
import com.srikanth.javareskill.mapper.EmployeeMapper;
import com.srikanth.javareskill.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for {@link EmployeeController} using standalone MockMvc.
 *
 * <p>No Spring application context is loaded — MockMvc is wired around the
 * controller and the {@link GlobalExceptionHandler} only, keeping tests fast.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeController")
class EmployeeControllerTest {

    @Mock  private EmployeeService  employeeService;
    @InjectMocks private EmployeeController controller;

    private MockMvc     mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper()
                .findAndRegisterModules();   // registers JavaTimeModule for LocalDate
    }

    // ── fixture ──────────────────────────────────────────────────────────────

    private static Employee sampleEmployee(String id) {
        return Employee.builder()
                .id(id)
                .name("Alice Smith")
                .email("alice@example.com")
                .departmentId("DEPT-01")
                .role(Role.ENGINEER)
                .salary(new BigDecimal("75000.00"))
                .status(EmployeeStatus.ACTIVE)
                .joiningDate(LocalDate.of(2024, 1, 15))
                .build();
    }

    private static CreateEmployeeRequest validCreateRequest() {
        return new CreateEmployeeRequest(
                "Alice Smith", "alice@example.com", "DEPT-01",
                Role.ENGINEER, new BigDecimal("75000.00"),
                EmployeeStatus.ACTIVE, LocalDate.of(2024, 1, 15));
    }

    // =========================================================================
    // POST /employees
    // =========================================================================

    @Nested
    @DisplayName("POST /employees")
    class CreateEmployee {

        @Test
        @DisplayName("valid request → 201 with Location header and generated ID")
        void validRequest_returns201() throws Exception {
            doNothing().when(employeeService).hire(any());

            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.message", containsString("created")));

            verify(employeeService).hire(any(Employee.class));
        }

        @Test
        @DisplayName("blank name → 400 with fieldErrors.name")
        void blankName_returns400() throws Exception {
            var req = validCreateRequest();
            req.setName("");

            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.name").isArray());
        }

        @Test
        @DisplayName("invalid email → 400 with fieldErrors.email")
        void invalidEmail_returns400() throws Exception {
            var req = validCreateRequest();
            req.setEmail("not-valid");

            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.email").isArray());
        }

        @Test
        @DisplayName("null salary → 400 with fieldErrors.salary")
        void nullSalary_returns400() throws Exception {
            var req = validCreateRequest();
            req.setSalary(null);

            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.salary").isArray());
        }

        @Test
        @DisplayName("negative salary → 400 with fieldErrors.salary")
        void negativeSalary_returns400() throws Exception {
            var req = validCreateRequest();
            req.setSalary(new BigDecimal("-1"));

            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.salary").isArray());
        }
    }

    // =========================================================================
    // GET /employees/{id}
    // =========================================================================

    @Nested
    @DisplayName("GET /employees/{id}")
    class GetEmployee {

        @Test
        @DisplayName("existing ID → 200 with employee body")
        void existingId_returns200() throws Exception {
            Employee emp = sampleEmployee("EMP-01");
            when(employeeService.findById(new EmployeeId("EMP-01")))
                    .thenReturn(Optional.of(emp));

            mockMvc.perform(get("/employees/EMP-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id",    is("EMP-01")))
                    .andExpect(jsonPath("$.name",  is("Alice Smith")))
                    .andExpect(jsonPath("$.email", is("alice@example.com")))
                    .andExpect(jsonPath("$.role",  is("ENGINEER")))
                    .andExpect(jsonPath("$.status",is("ACTIVE")));
        }

        @Test
        @DisplayName("unknown ID → 404 with error body")
        void unknownId_returns404() throws Exception {
            when(employeeService.findById(any())).thenReturn(Optional.empty());

            mockMvc.perform(get("/employees/GHOST-99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", containsString("GHOST-99")));
        }
    }

    // =========================================================================
    // GET /employees  (with filters)
    // =========================================================================

    @Nested
    @DisplayName("GET /employees (filters)")
    class ListEmployees {

        @Test
        @DisplayName("no filters → 200 with all employees")
        void noFilters_returnsAll() throws Exception {
            when(employeeService.findAll())
                    .thenReturn(List.of(sampleEmployee("E1"), sampleEmployee("E2")));

            mockMvc.perform(get("/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("departmentId filter → only matching employees returned")
        void departmentFilter_returnsFiltered() throws Exception {
            Employee inDept  = sampleEmployee("E1");   // departmentId = "DEPT-01"
            Employee outDept = Employee.builder()
                    .id("E2").name("Bob").email("bob@x.com")
                    .departmentId("DEPT-99").role(Role.MANAGER)
                    .salary(BigDecimal.TEN).status(EmployeeStatus.ACTIVE)
                    .joiningDate(LocalDate.now()).build();

            when(employeeService.findAll()).thenReturn(List.of(inDept, outDept));

            mockMvc.perform(get("/employees").param("departmentId", "DEPT-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is("E1")));
        }

        @Test
        @DisplayName("status=INACTIVE filter → only inactive employees returned")
        void statusFilter_returnsFiltered() throws Exception {
            Employee active   = sampleEmployee("E1");
            Employee inactive = Employee.builder()
                    .id("E2").name("Carol").email("carol@x.com")
                    .departmentId("DEPT-01").role(Role.ANALYST)
                    .salary(BigDecimal.TEN).status(EmployeeStatus.INACTIVE)
                    .joiningDate(LocalDate.now()).build();

            when(employeeService.findAll()).thenReturn(List.of(active, inactive));

            mockMvc.perform(get("/employees").param("status", "INACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is("E2")));
        }

        @Test
        @DisplayName("departmentId + status filters combined → AND semantics")
        void combinedFilters_andSemantics() throws Exception {
            Employee match = sampleEmployee("E1");   // DEPT-01, ACTIVE
            Employee wrongDept = Employee.builder()
                    .id("E2").name("Bob").email("b@x.com").departmentId("DEPT-99")
                    .role(Role.HR).salary(BigDecimal.TEN)
                    .status(EmployeeStatus.ACTIVE).joiningDate(LocalDate.now()).build();
            Employee wrongStatus = Employee.builder()
                    .id("E3").name("Carol").email("c@x.com").departmentId("DEPT-01")
                    .role(Role.HR).salary(BigDecimal.TEN)
                    .status(EmployeeStatus.INACTIVE).joiningDate(LocalDate.now()).build();

            when(employeeService.findAll()).thenReturn(List.of(match, wrongDept, wrongStatus));

            mockMvc.perform(get("/employees")
                            .param("departmentId", "DEPT-01")
                            .param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is("E1")));
        }

        @Test
        @DisplayName("no matches → 200 with empty list")
        void noMatches_returnsEmptyList() throws Exception {
            when(employeeService.findAll()).thenReturn(List.of(sampleEmployee("E1")));

            mockMvc.perform(get("/employees").param("departmentId", "DEPT-NONE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // =========================================================================
    // PUT /employees/{id}/status
    // =========================================================================

    @Nested
    @DisplayName("PUT /employees/{id}/status")
    class UpdateStatus {

        @Test
        @DisplayName("existing employee → 200 with updated status")
        void existingEmployee_returns200WithUpdatedStatus() throws Exception {
            Employee emp = sampleEmployee("EMP-01");
            when(employeeService.findById(new EmployeeId("EMP-01")))
                    .thenReturn(Optional.of(emp));
            doNothing().when(employeeService).update(any());

            var body = new UpdateEmployeeStatusRequest(EmployeeStatus.INACTIVE);

            mockMvc.perform(put("/employees/EMP-01/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id",     is("EMP-01")))
                    .andExpect(jsonPath("$.status", is("INACTIVE")));

            verify(employeeService).update(any(Employee.class));
        }

        @Test
        @DisplayName("unknown employee → 404")
        void unknownEmployee_returns404() throws Exception {
            when(employeeService.findById(any())).thenReturn(Optional.empty());

            var body = new UpdateEmployeeStatusRequest(EmployeeStatus.INACTIVE);

            mockMvc.perform(put("/employees/GHOST/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("null status in body → 400")
        void nullStatus_returns400() throws Exception {
            mockMvc.perform(put("/employees/EMP-01/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.status").isArray());
        }
    }
}

