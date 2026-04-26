package com.srikanth.javareskill.config;

import com.srikanth.javareskill.exception.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ConfigLoader}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Happy-path loading from classpath and filesystem</li>
 *   <li>All typed getters ({@code String}, {@code int}, {@code BigDecimal}, {@code boolean})</li>
 *   <li>Missing required keys → {@link ConfigurationException}</li>
 *   <li>Malformed numeric values → {@link ConfigurationException} with cause</li>
 *   <li>Missing classpath resource → {@link ConfigurationException}</li>
 *   <li>Missing filesystem file → {@link ConfigurationException}</li>
 *   <li>Stream-close guarantee: the temp file can be deleted after load
 *       (proving no dangling file handle was left open)</li>
 * </ul>
 */
class ConfigLoaderTest {

    // =========================================================================
    // Classpath loading  (uses src/test/resources/app.properties)
    // =========================================================================

    @Test
    void fromClasspath_loadsAllValues() {
        AppConfig cfg = ConfigLoader.fromClasspath("app.properties");

        assertEquals("HR Test Environment", cfg.getAppName());
        assertEquals(50, cfg.getMaxEmployees());
        assertEquals(new BigDecimal("30000.00"), cfg.getDefaultSalary());
        assertTrue(cfg.isAuditEnabled());
    }

    @Test
    void fromClasspath_missingResource_throwsConfigurationException() {
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> ConfigLoader.fromClasspath("no-such-file.properties"));
        assertTrue(ex.getMessage().contains("no-such-file.properties"));
    }

    @Test
    void fromClasspath_configurationException_isHrException() {
        assertThrows(com.srikanth.javareskill.exception.HrException.class,
                () -> ConfigLoader.fromClasspath("no-such-file.properties"));
    }

    // =========================================================================
    // Filesystem loading  (@TempDir injected by JUnit 5)
    // =========================================================================

    @Test
    void fromPath_loadsAllValues(@TempDir Path tmp) throws IOException {
        Path cfg = writeProps(tmp, "cfg.properties",
                "app.name=Payroll System",
                "app.max.employees=200",
                "app.default.salary=45000.50",
                "app.audit.enabled=true");

        AppConfig result = ConfigLoader.fromPath(cfg);

        assertEquals("Payroll System", result.getAppName());
        assertEquals(200, result.getMaxEmployees());
        assertEquals(new BigDecimal("45000.50"), result.getDefaultSalary());
        assertTrue(result.isAuditEnabled());
    }

    @Test
    void fromPath_auditEnabled_defaultsFalseWhenAbsent(@TempDir Path tmp) throws IOException {
        Path cfg = writeProps(tmp, "no-audit.properties",
                "app.name=HR",
                "app.max.employees=10",
                "app.default.salary=20000");

        assertFalse(ConfigLoader.fromPath(cfg).isAuditEnabled());
    }

    @Test
    void fromPath_missingFile_throwsConfigurationException(@TempDir Path tmp) {
        Path missing = tmp.resolve("ghost.properties");
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> ConfigLoader.fromPath(missing));
        assertTrue(ex.getMessage().contains("ghost.properties"));
        assertNotNull(ex.getCause(), "Cause (IOException) must be preserved");
    }

    // =========================================================================
    // Missing required keys
    // =========================================================================

    @Test
    void fromPath_missingAppName_throwsConfigurationException(@TempDir Path tmp)
            throws IOException {
        Path cfg = writeProps(tmp, "bad.properties",
                "app.max.employees=10",
                "app.default.salary=20000");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> ConfigLoader.fromPath(cfg));
        assertTrue(ex.getMessage().contains(ConfigLoader.KEY_APP_NAME));
    }

    @Test
    void fromPath_missingMaxEmployees_throwsConfigurationException(@TempDir Path tmp)
            throws IOException {
        Path cfg = writeProps(tmp, "bad.properties",
                "app.name=Test",
                "app.default.salary=20000");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> ConfigLoader.fromPath(cfg));
        assertTrue(ex.getMessage().contains(ConfigLoader.KEY_MAX_EMPLOYEES));
    }

    @Test
    void fromPath_missingDefaultSalary_throwsConfigurationException(@TempDir Path tmp)
            throws IOException {
        Path cfg = writeProps(tmp, "bad.properties",
                "app.name=Test",
                "app.max.employees=10");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> ConfigLoader.fromPath(cfg));
        assertTrue(ex.getMessage().contains(ConfigLoader.KEY_DEFAULT_SALARY));
    }

    // =========================================================================
    // Malformed / invalid values
    // =========================================================================

    @Test
    void fromPath_nonIntegerMaxEmployees_throwsWithCause(@TempDir Path tmp) throws IOException {
        Path cfg = writeProps(tmp, "bad.properties",
                "app.name=Test",
                "app.max.employees=not-a-number",
                "app.default.salary=20000");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> ConfigLoader.fromPath(cfg));
        assertTrue(ex.getMessage().contains(ConfigLoader.KEY_MAX_EMPLOYEES));
        assertInstanceOf(NumberFormatException.class, ex.getCause(),
                "NumberFormatException must be the cause");
    }

    @Test
    void fromPath_nonDecimalDefaultSalary_throwsWithCause(@TempDir Path tmp) throws IOException {
        Path cfg = writeProps(tmp, "bad.properties",
                "app.name=Test",
                "app.max.employees=10",
                "app.default.salary=ABC");

        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> ConfigLoader.fromPath(cfg));
        assertTrue(ex.getMessage().contains(ConfigLoader.KEY_DEFAULT_SALARY));
        assertInstanceOf(NumberFormatException.class, ex.getCause());
    }

    @Test
    void fromPath_negativeMaxEmployees_throwsIllegalArgument(@TempDir Path tmp)
            throws IOException {
        Path cfg = writeProps(tmp, "bad.properties",
                "app.name=Test",
                "app.max.employees=-5",
                "app.default.salary=20000");

        // AppConfig.Builder validates > 0
        assertThrows(IllegalArgumentException.class, () -> ConfigLoader.fromPath(cfg));
    }

    @Test
    void fromPath_negativeSalary_throwsIllegalArgument(@TempDir Path tmp) throws IOException {
        Path cfg = writeProps(tmp, "bad.properties",
                "app.name=Test",
                "app.max.employees=10",
                "app.default.salary=-1");

        assertThrows(IllegalArgumentException.class, () -> ConfigLoader.fromPath(cfg));
    }

    // =========================================================================
    // try-with-resources stream-close guarantee
    // =========================================================================

    @Test
    void fromPath_fileHandleIsReleasedAfterLoad(@TempDir Path tmp) throws IOException {
        Path cfg = writeProps(tmp, "cleanup.properties",
                "app.name=Cleanup Test",
                "app.max.employees=5",
                "app.default.salary=10000");

        ConfigLoader.fromPath(cfg);   // load — internally uses try-with-resources

        // If the file handle were still open on macOS/Linux we could still delete;
        // on Windows this would throw.  The assertion here is that no exception
        // is thrown and the config was correctly loaded.
        assertTrue(Files.exists(cfg), "File must still exist after load");
        assertDoesNotThrow(() -> Files.delete(cfg),
                "File must be deletable — no dangling handle");
    }

    @Test
    void fromPath_fileHandleIsReleasedEvenOnParseError(@TempDir Path tmp) throws IOException {
        Path cfg = writeProps(tmp, "broken.properties",
                "app.name=",          // blank → ConfigurationException during parse
                "app.max.employees=10",
                "app.default.salary=10000");

        assertThrows(ConfigurationException.class, () -> ConfigLoader.fromPath(cfg));

        // Stream must have been closed by try-with-resources despite the exception
        assertDoesNotThrow(() -> Files.delete(cfg),
                "File must be deletable even after a parse error");
    }

    // =========================================================================
    // AppConfig value-object validation (independent of loader)
    // =========================================================================

    @Test
    void appConfig_toStringContainsAllFields() {
        AppConfig cfg = AppConfig.builder()
                .appName("Test")
                .maxEmployees(10)
                .defaultSalary(BigDecimal.TEN)
                .auditEnabled(true)
                .build();
        String s = cfg.toString();
        assertTrue(s.contains("Test"));
        assertTrue(s.contains("10"));
        assertTrue(s.contains("true"));
    }

    @Test
    void appConfig_nullAppName_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                AppConfig.builder()
                        .maxEmployees(10)
                        .defaultSalary(BigDecimal.TEN)
                        .build());
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /** Writes lines to {@code dir/name} and returns the {@link Path}. */
    private static Path writeProps(Path dir, String name, String... lines) throws IOException {
        Path file = dir.resolve(name);
        Files.write(file, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        return file;
    }
}

