package com.srikanth.javareskill.config;

import com.srikanth.javareskill.exception.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads {@link AppConfig} from a {@code .properties} file.
 *
 * <h2>Loading strategies</h2>
 * <ol>
 *   <li>{@link #fromClasspath(String)} — reads a resource from the classpath
 *       (e.g. {@code "app.properties"} bundled inside the JAR / test-resources).</li>
 *   <li>{@link #fromPath(Path)} — reads an arbitrary file on the filesystem.</li>
 * </ol>
 *
 * <h2>try-with-resources</h2>
 * Every {@link InputStream} and {@link Reader} opened by this class is declared
 * inside a {@code try}-with-resources block, guaranteeing that:
 * <ul>
 *   <li>The stream is closed even if parsing throws.</li>
 *   <li>No file handles are leaked on exception paths.</li>
 * </ul>
 *
 * <h2>Config keys</h2>
 * <pre>
 * app.name            (String,     required)
 * app.max.employees   (int > 0,    required)
 * app.default.salary  (BigDecimal >= 0, required)
 * app.audit.enabled   (boolean,    optional — defaults to false)
 * </pre>
 */
public final class ConfigLoader {

    // Config key constants — single source of truth
    static final String KEY_APP_NAME        = "app.name";
    static final String KEY_MAX_EMPLOYEES   = "app.max.employees";
    static final String KEY_DEFAULT_SALARY  = "app.default.salary";
    static final String KEY_AUDIT_ENABLED   = "app.audit.enabled";

    private ConfigLoader() { /* utility class — no instances */ }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Loads {@link AppConfig} from a classpath resource.
     *
     * <p>The resource is located using the thread-context class-loader, falling
     * back to {@link ConfigLoader}'s own class-loader if necessary.</p>
     *
     * @param resourceName classpath resource name, e.g. {@code "app.properties"}
     * @return populated {@link AppConfig}
     * @throws ConfigurationException if the resource is not found, cannot be read,
     *                                or contains invalid values
     */
    public static AppConfig fromClasspath(String resourceName) {
        // ── try-with-resources: InputStream ──────────────────────────────────
        try (InputStream raw = openClasspathResource(resourceName);
             Reader reader   = new InputStreamReader(raw, StandardCharsets.UTF_8)) {

            return parse(resourceName, loadProperties(resourceName, reader));

        } catch (IOException e) {
            throw new ConfigurationException(resourceName,
                    "Failed to read classpath resource", e);
        }
    }

    /**
     * Loads {@link AppConfig} from an arbitrary filesystem {@link Path}.
     *
     * @param path absolute or relative path to a {@code .properties} file
     * @return populated {@link AppConfig}
     * @throws ConfigurationException if the file is not found, cannot be read,
     *                                or contains invalid values
     */
    public static AppConfig fromPath(Path path) {
        String label = path.toString();

        // ── try-with-resources: BufferedReader provided by Files.newBufferedReader ─
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

            return parse(label, loadProperties(label, reader));

        } catch (IOException e) {
            throw new ConfigurationException(label,
                    "Failed to read configuration file", e);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /** Opens a classpath resource, throwing {@link ConfigurationException} if absent. */
    private static InputStream openClasspathResource(String name) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = (cl != null) ? cl.getResourceAsStream(name) : null;
        if (in == null) {
            in = ConfigLoader.class.getClassLoader().getResourceAsStream(name);
        }
        if (in == null) {
            throw new ConfigurationException(name,
                    "Classpath resource not found: " + name);
        }
        return in;
    }

    /**
     * Loads key/value pairs from {@code reader} into a {@link Properties} object.
     *
     * <p>The reader is intentionally NOT closed here — the caller's
     * try-with-resources block owns its lifecycle.</p>
     */
    private static Properties loadProperties(String label, Reader reader) throws IOException {
        Properties props = new Properties();
        props.load(reader);   // may throw IOException — propagated to caller
        return props;
    }

    /** Converts raw {@link Properties} into a validated {@link AppConfig}. */
    private static AppConfig parse(String label, Properties props) {
        return AppConfig.builder()
                .appName(requireString(label, props, KEY_APP_NAME))
                .maxEmployees(requireInt(label, props, KEY_MAX_EMPLOYEES))
                .defaultSalary(requireDecimal(label, props, KEY_DEFAULT_SALARY))
                .auditEnabled(optionalBoolean(props, KEY_AUDIT_ENABLED, false))
                .build();
    }

    // --- typed extraction helpers -------------------------------------------

    private static String requireString(String label, Properties props, String key) {
        String v = props.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new ConfigurationException(label,
                    "Required key '" + key + "' is missing or blank");
        }
        return v.strip();
    }

    private static int requireInt(String label, Properties props, String key) {
        String raw = requireString(label, props, key);
        try {
            return Integer.parseInt(raw.strip());
        } catch (NumberFormatException e) {
            throw new ConfigurationException(label,
                    "Key '" + key + "' must be an integer, got: '" + raw + "'", e);
        }
    }

    private static BigDecimal requireDecimal(String label, Properties props, String key) {
        String raw = requireString(label, props, key);
        try {
            return new BigDecimal(raw.strip());
        } catch (NumberFormatException e) {
            throw new ConfigurationException(label,
                    "Key '" + key + "' must be a decimal number, got: '" + raw + "'", e);
        }
    }

    private static boolean optionalBoolean(Properties props, String key, boolean defaultValue) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw.strip());
    }
}

