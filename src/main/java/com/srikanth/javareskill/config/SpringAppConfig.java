package com.srikanth.javareskill.config;

import com.srikanth.javareskill.payroll.PayrollService;
import com.srikanth.javareskill.payroll.impl.PayrollServiceImpl;
import com.srikanth.javareskill.repository.DepartmentRepository;
import com.srikanth.javareskill.repository.EmployeeRepository;
import com.srikanth.javareskill.repository.InMemoryPayrollRepository;
import com.srikanth.javareskill.repository.inmemory.InMemoryDepartmentRepository;
import com.srikanth.javareskill.repository.inmemory.InMemoryEmployeeRepository;
import com.srikanth.javareskill.service.DepartmentService;
import com.srikanth.javareskill.service.EmployeeService;
import com.srikanth.javareskill.service.impl.DepartmentServiceImpl;
import com.srikanth.javareskill.service.impl.EmployeeServiceImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring bean configuration that wires the in-memory repositories and services
 * needed by the REST controllers.
 *
 * <h2>Why manual {@code @Bean} declarations?</h2>
 * <p>The domain classes (services, repositories) are plain Java — they carry no
 * Spring annotations so they remain framework-agnostic and unit-testable without
 * a Spring context.  This class is the single integration point that bridges the
 * two worlds.</p>
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class SpringAppConfig {

    // ── Repositories ─────────────────────────────────────────────────────────

    /**
     * Single shared in-memory employee store for the lifetime of the application.
     */
    @Bean
    public EmployeeRepository employeeRepository() {
        return new InMemoryEmployeeRepository();
    }

    /**
     * Single shared in-memory department store for the lifetime of the application.
     */
    @Bean
    public DepartmentRepository departmentRepository() {
        return new InMemoryDepartmentRepository();
    }

    // ── Services ─────────────────────────────────────────────────────────────

    @Bean
    public EmployeeService employeeService(EmployeeRepository employeeRepository) {
        return new EmployeeServiceImpl(employeeRepository);
    }

    @Bean
    public DepartmentService departmentService(DepartmentRepository departmentRepository) {
        return new DepartmentServiceImpl(departmentRepository);
    }

    @Bean
    public PayrollService payrollService() {
        return new PayrollServiceImpl();
    }

    @Bean
    public InMemoryPayrollRepository payrollRepository() {
        return new InMemoryPayrollRepository();
    }
}
