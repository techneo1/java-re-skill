package com.srikanth.javareskill;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for Spring Boot integration tests.
 *
 * <h2>What this provides</h2>
 * <ul>
 *   <li>{@code @SpringBootTest(webEnvironment = RANDOM_PORT)} — boots the full
 *       application context on a free port so tests never clash with each other
 *       or a running local server.</li>
 *   <li>{@code @ActiveProfiles("test")} — activates {@code application-test.properties},
 *       which configures:
 *       <ul>
 *         <li>An isolated H2 in-memory database ({@code hrdb-test})</li>
 *         <li>Minimal logging (INFO / WARN)</li>
 *         <li>No DevTools restart / live-reload</li>
 *         <li>Error responses without stack-traces (mirrors production)</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * class EmployeeControllerTest extends BaseIntegrationTest {
 *
 *     @Autowired
 *     private TestRestTemplate rest;
 *
 *     @Test
 *     void createEmployee_returns201() { ... }
 * }
 * }</pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    // Extend this class to inherit the full wired Spring context on a random port
    // with the 'test' profile active.
}

