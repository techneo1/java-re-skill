package com.srikanth.javareskill.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository contract for basic CRUD operations over any entity type.
 *
 * <h2>Type parameters</h2>
 * <ul>
 *   <li>{@code T}  – the entity type (e.g. {@code Employee}, {@code Department})</li>
 *   <li>{@code ID} – the key type used to identify entities (e.g. {@code EmployeeId},
 *       {@code String}).  Must correctly implement {@link Object#equals(Object)} and
 *       {@link Object#hashCode()} to work reliably as a {@link java.util.HashMap} key.</li>
 * </ul>
 *
 * <h2>Design goals</h2>
 * <ul>
 *   <li>Single interface shared by every domain repository — eliminates boilerplate.</li>
 *   <li>Domain-specific repositories (e.g. {@code EmployeeRepository}) extend this
 *       interface with additional query methods without re-declaring the CRUD contract.</li>
 *   <li>The concrete implementation ({@link InMemoryRepository}) is kept separate from
 *       the interface, allowing multiple implementations (in-memory, JDBC, …).</li>
 * </ul>
 *
 * @param <T>  entity type
 * @param <ID> identifier type
 */
public interface GenericRepository<T, ID> {

    /**
     * Persists a new entity.
     *
     * @param entity the entity to save; must not be {@code null}
     * @throws IllegalArgumentException if an entity with the same ID already exists
     */
    void save(T entity);

    /**
     * Looks up an entity by its identifier.
     *
     * @param id the entity identifier; must not be {@code null}
     * @return an {@link Optional} containing the entity, or {@link Optional#empty()} if absent
     */
    Optional<T> findById(ID id);

    /**
     * Returns an unmodifiable snapshot of all entities in insertion order.
     *
     * @return immutable list; never {@code null}
     */
    List<T> findAll();

    /**
     * Replaces an existing entity matched by its ID.
     *
     * @param entity the updated entity; must not be {@code null}
     * @throws RuntimeException (sub-type determined by the implementation) if the
     *                          entity does not exist
     */
    void update(T entity);

    /**
     * Removes the entity with the given identifier.
     *
     * @param id the entity identifier; must not be {@code null}
     * @throws RuntimeException (sub-type determined by the implementation) if the
     *                          entity does not exist
     */
    void deleteById(ID id);

    /**
     * Returns {@code true} if an entity with the given identifier is present.
     *
     * @param id the entity identifier; must not be {@code null}
     */
    boolean existsById(ID id);

    /**
     * Returns the number of entities currently held in the repository.
     */
    int count();
}

