package com.srikanth.javareskill.payroll.strategy;

import com.srikanth.javareskill.domain.enums.Role;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Factory that resolves the default {@link TaxStrategy} for a given employee {@link Role}.
 *
 * <p>Uses a <strong>switch expression</strong> (JDK 14+) to map every
 * {@link Role} constant to its default {@link TaxStrategy}.  The switch
 * is exhaustive — a compile-time error occurs if a new constant is added
 * to {@link Role} without a corresponding arm here.</p>
 *
 * <ul>
 *   <li>DIRECTOR / SENIOR_MANAGER → Progressive (multi-bracket)</li>
 *   <li>MANAGER / SENIOR_ENGINEER → Flat 20 %</li>
 *   <li>ENGINEER / ANALYST / HR   → Flat 10 %</li>
 * </ul>
 *
 * <h2>Java switch-expression features demonstrated</h2>
 * <ul>
 *   <li>Arrow ({@code ->}) syntax — no fall-through, no {@code break}</li>
 *   <li>Multi-label arms ({@code case A, B ->}) for roles that share a strategy</li>
 *   <li>Exhaustiveness — every {@link Role} constant is covered; no {@code default}
 *       is required, and adding a new enum constant forces a compile error</li>
 *   <li>Expression result returned directly</li>
 * </ul>
 *
 * <h2>SOLID principles applied</h2>
 * <ul>
 *   <li><b>O – Open/Closed</b>: The built-in switch is <em>closed</em> for
 *       modification.  The factory is <em>open for extension</em> via
 *       {@link #register(Role, TaxStrategy)}, which lets callers override
 *       any mapping at runtime without editing this class.</li>
 * </ul>
 */
public final class TaxStrategyFactory {

    /**
     * Optional override registry.  Entries here take precedence over the
     * built-in switch expression, enabling runtime extensibility (OCP).
     */
    private static final Map<Role, TaxStrategy> OVERRIDES = new HashMap<>();

    private TaxStrategyFactory() {}

    /**
     * Returns the {@link TaxStrategy} for the given role.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>If a custom strategy was registered via
     *       {@link #register(Role, TaxStrategy)}, that strategy is returned.</li>
     *   <li>Otherwise the built-in <strong>switch expression</strong> is evaluated.
     *       It is exhaustive over all {@link Role} constants — no {@code default}
     *       arm is needed, and the compiler will flag any missing constant.</li>
     * </ol>
     *
     * @param role the employee's role; must not be {@code null}
     * @return the resolved strategy; never {@code null}
     */
    public static TaxStrategy forRole(Role role) {
        Objects.requireNonNull(role, "role must not be null");

        // 1. Check runtime overrides first (OCP extensibility)
        TaxStrategy override = OVERRIDES.get(role);
        if (override != null) {
            return override;
        }

        // 2. Built-in mapping via switch expression (exhaustive, no default)
        return switch (role) {
            case DIRECTOR, SENIOR_MANAGER -> new ProgressiveTaxStrategy();
            case MANAGER, SENIOR_ENGINEER -> new FlatRateTaxStrategy(0.20);
            case ENGINEER, ANALYST, HR    -> new FlatRateTaxStrategy(0.10);
        };
    }

    /**
     * Registers (or replaces) the {@link TaxStrategy} for the given {@link Role}.
     *
     * <p><b>OCP – Open for Extension</b>: Use this method to override the
     * built-in switch mapping at runtime, without modifying this class.</p>
     *
     * @param role     the role to map; must not be {@code null}
     * @param strategy the strategy to associate; must not be {@code null}
     */
    public static void register(Role role, TaxStrategy strategy) {
        Objects.requireNonNull(role,     "role must not be null");
        Objects.requireNonNull(strategy, "strategy must not be null");
        OVERRIDES.put(role, strategy);
    }

    /**
     * Removes all registered overrides, reverting every role back to its
     * built-in switch-expression default.
     */
    public static void resetOverrides() {
        OVERRIDES.clear();
    }
}
