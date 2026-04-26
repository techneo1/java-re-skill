package com.srikanth.javareskill.repository;

import java.util.*;
import java.util.function.Function;

/**
 * Thread-unsafe, in-memory implementation of {@link GenericRepository}.
 *
 * <p>Subclasses supply two strategies via the constructor:</p>
 * <ol>
 *   <li><b>keyExtractor</b> – a {@link Function}{@code <T, ID>} that extracts the
 *       entity's identifier (e.g. {@code Employee::getId} wrapped in an
 *       {@code EmployeeId}).  This keeps the generic base class decoupled from
 *       any specific domain type.</li>
 *   <li><b>notFoundFactory</b> – a {@link Function}{@code <ID, RuntimeException>}
 *       that constructs the right domain exception when an entity is missing
 *       (e.g. {@code id -> new EmployeeNotFoundException(id.getValue())}).
 *       Allows each repository to throw its own typed exception without
 *       overriding every mutating method.</li>
 * </ol>
 *
 * <h2>Internal data structures</h2>
 * <ul>
 *   <li>{@link HashMap}{@code <ID, T>}  – O(1) look-up by ID</li>
 *   <li>{@link ArrayList}{@code <T>}    – preserves insertion order for listing</li>
 * </ul>
 *
 * @param <T>  entity type
 * @param <ID> identifier type — must implement {@code equals} / {@code hashCode}
 */
public abstract class InMemoryRepository<T, ID> implements GenericRepository<T, ID> {

    // ── Internal state ────────────────────────────────────────────────────────

    /** Primary O(1) look-up structure. */
    private final Map<ID, T> idIndex = new HashMap<>();

    /** Insertion-ordered list for {@link #findAll()}. */
    private final List<T> entityList = new ArrayList<>();

    // ── Strategy functions supplied by subclasses ─────────────────────────────

    /** Extracts the identifier from an entity. */
    private final Function<T, ID> keyExtractor;

    /** Produces the exception to throw when an entity with a given ID is missing. */
    private final Function<ID, RuntimeException> notFoundFactory;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * @param keyExtractor    extracts the ID from an entity instance
     * @param notFoundFactory creates the domain exception for a missing ID
     */
    protected InMemoryRepository(Function<T, ID> keyExtractor,
                                  Function<ID, RuntimeException> notFoundFactory) {
        this.keyExtractor     = Objects.requireNonNull(keyExtractor,     "keyExtractor");
        this.notFoundFactory  = Objects.requireNonNull(notFoundFactory,  "notFoundFactory");
    }

    // =========================================================================
    // GenericRepository implementation
    // =========================================================================

    @Override
    public void save(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        ID key = keyExtractor.apply(entity);
        if (idIndex.containsKey(key)) {
            throw new IllegalArgumentException(
                    "Entity with ID '" + key + "' already exists");
        }
        idIndex.put(key, entity);
        entityList.add(entity);
    }

    @Override
    public Optional<T> findById(ID id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(idIndex.get(id));
    }

    @Override
    public List<T> findAll() {
        return Collections.unmodifiableList(entityList);
    }

    @Override
    public void update(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        ID key = keyExtractor.apply(entity);
        if (!idIndex.containsKey(key)) {
            throw notFoundFactory.apply(key);
        }
        idIndex.put(key, entity);
        int idx = listIndexOf(key);
        entityList.set(idx, entity);
    }

    @Override
    public void deleteById(ID id) {
        Objects.requireNonNull(id, "id must not be null");
        T removed = idIndex.remove(id);
        if (removed == null) {
            throw notFoundFactory.apply(id);
        }
        entityList.remove(removed);
    }

    @Override
    public boolean existsById(ID id) {
        Objects.requireNonNull(id, "id must not be null");
        return idIndex.containsKey(id);
    }

    @Override
    public int count() {
        return idIndex.size();
    }

    // =========================================================================
    // Protected helpers available to subclasses
    // =========================================================================

    /**
     * Returns a mutable view of the internal index, restricted to read operations.
     * Subclasses can use this for custom query methods (e.g. filter by department).
     */
    protected Map<ID, T> indexView() {
        return Collections.unmodifiableMap(idIndex);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private int listIndexOf(ID key) {
        for (int i = 0; i < entityList.size(); i++) {
            if (keyExtractor.apply(entityList.get(i)).equals(key)) {
                return i;
            }
        }
        throw new IllegalStateException("Repository in inconsistent state for key: " + key);
    }
}

