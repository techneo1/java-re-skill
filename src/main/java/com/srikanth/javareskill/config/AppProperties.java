package com.srikanth.javareskill.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

/**
 * Typed binding for the {@code app.*} namespace defined in every profile's
 * {@code application-{profile}.properties} file.
 *
 * <h2>Benefits over {@code @Value}</h2>
 * <ul>
 *   <li>IDE auto-completion and Javadoc directly on the fields.</li>
 *   <li>Compile-time safety — typos in property names fail at startup.</li>
 *   <li>Easy to inject into any Spring component without the {@code $\{...\}} syntax.</li>
 *   <li>Simple to test: instantiate directly in unit tests without a Spring context.</li>
 * </ul>
 *
 * <h2>How to use</h2>
 * <pre>{@code
 * @Autowired
 * private AppProperties appProperties;
 *
 * String env = appProperties.env();          // "dev" | "test" | "prod"
 * int    max = appProperties.maxEmployees(); // 1000 (dev), 50 (test)
 * }</pre>
 *
 * <p>Registered as a bean via {@link SpringAppConfig#appProperties()}.</p>
 *
 * @param env            active environment label (dev / test / prod)
 * @param maxEmployees   upper limit for employee count in this environment
 * @param defaultSalary  fallback salary used when none is provided
 * @param auditEnabled   whether audit logging is active in this environment
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("dev")          String     env,
        @DefaultValue("1000")         int        maxEmployees,
        @DefaultValue("50000.00")     BigDecimal defaultSalary,
        @DefaultValue("true")         boolean    auditEnabled
) {}

