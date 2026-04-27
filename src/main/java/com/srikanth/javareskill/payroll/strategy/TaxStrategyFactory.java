package com.srikanth.javareskill.payroll.strategy;

import com.srikanth.javareskill.domain.enums.Role;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Factory that resolves the default {@link TaxStrategy} for a given employee {@link Role}.
 *
 * <p>This keeps the mapping knowledge in one place and avoids scattering
 * {@code if/switch} blocks across the service layer.</p>
 *
 * <ul>
 *   <li>DIRECTOR / SENIOR_MANAGER → Progressive (multi-bracket)</li>
 *   <li>MANAGER / SENIOR_ENGINEER → Flat 20 %</li>
 *   <li>ENGINEER / ANALYST / HR   → Flat 10 %</li>
 * </ul>
 *
 * <h2>SOLID principles applied</h2>
 * <ul>
 *   <li><b>O – Open/Closed</b>: The built-in role→strategy mappings are closed
 *       for modification, but the factory is <em>open for extension</em> via
 *       {@link #register(Role, TaxStrategy)}.  Callers can add support for new
 *       roles or override an existing mapping <strong>without editing this class</strong>.
 *   </li>
 * </ul>
 */
public final class TaxStrategyFactory {

    /**
     * Mutable registry that starts with the built-in defaults and can be
     * extended via {@link #register(Role, TaxStrategy)} (OCP).
     */
    private static final Map<Role, TaxStrategy> STRATEGY_BY_ROLE = new HashMap<>(Map.of(
            Role.DIRECTOR,        new ProgressiveTaxStrategy(),
            Role.SENIOR_MANAGER,  new ProgressiveTaxStrategy(),
            Role.MANAGER,         new FlatRateTaxStrategy(0.20),
            Role.SENIOR_ENGINEER, new FlatRateTaxStrategy(0.20),
            Role.ENGINEER,        new FlatRateTaxStrategy(0.10),
            Role.ANALYST,         new FlatRateTaxStrategy(0.10),
            Role.HR,              new FlatRateTaxStrategy(0.10)
    ));

    private TaxStrategyFactory() {}

    /**
     * Returns the {@link TaxStrategy} registered for the given role.
     *
     * @param role the employee's role; must not be {@code null}
     * @return the resolved strategy; never {@code null}
     * @throws IllegalArgumentException if no strategy is registered for {@code role}
     */
    public static TaxStrategy forRole(Role role) {
        Objects.requireNonNull(role, "role must not be null");
        TaxStrategy strategy = STRATEGY_BY_ROLE.get(role);
        if (strategy == null) {
            throw new IllegalArgumentException("No tax strategy configured for role: " + role);
        }
        return strategy;
    }

    /**
     * Registers (or replaces) the {@link TaxStrategy} for the given {@link Role}.
     *
     * <p><b>OCP – Open for Extension</b>: Use this method to add support for a
     * new role, or to substitute a custom strategy for an existing one, without
     * modifying the factory's source code.</p>
     *
     * <pre>{@code
     * // Example: override Engineer to use a flat 15% strategy
     * TaxStrategyFactory.register(Role.ENGINEER, new FlatRateTaxStrategy(0.15));
     * }</pre>
     *
     * @param role     the role to map; must not be {@code null}
     * @param strategy the strategy to associate; must not be {@code null}
     */
    public static void register(Role role, TaxStrategy strategy) {
        Objects.requireNonNull(role,     "role must not be null");
        Objects.requireNonNull(strategy, "strategy must not be null");
        STRATEGY_BY_ROLE.put(role, strategy);
    }
}

