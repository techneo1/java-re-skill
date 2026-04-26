package com.srikanth.javareskill.payroll.strategy;

import com.srikanth.javareskill.domain.enums.Role;

import java.util.Map;

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
 */
public final class TaxStrategyFactory {

    private static final Map<Role, TaxStrategy> STRATEGY_BY_ROLE = Map.of(
            Role.DIRECTOR,        new ProgressiveTaxStrategy(),
            Role.SENIOR_MANAGER,  new ProgressiveTaxStrategy(),
            Role.MANAGER,         new FlatRateTaxStrategy(0.20),
            Role.SENIOR_ENGINEER, new FlatRateTaxStrategy(0.20),
            Role.ENGINEER,        new FlatRateTaxStrategy(0.10),
            Role.ANALYST,         new FlatRateTaxStrategy(0.10),
            Role.HR,              new FlatRateTaxStrategy(0.10)
    );

    private TaxStrategyFactory() {}

    /**
     * Returns the default {@link TaxStrategy} for the given role.
     *
     * @param role the employee's role; must not be {@code null}
     * @return the resolved strategy; never {@code null}
     */
    public static TaxStrategy forRole(Role role) {
        TaxStrategy strategy = STRATEGY_BY_ROLE.get(role);
        if (strategy == null) {
            throw new IllegalArgumentException("No tax strategy configured for role: " + role);
        }
        return strategy;
    }
}

