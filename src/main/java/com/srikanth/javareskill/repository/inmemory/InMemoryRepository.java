package com.srikanth.javareskill.repository.inmemory;

import com.srikanth.javareskill.repository.GenericRepository;

import java.util.*;
import java.util.function.Function;

/**
 * Thread-unsafe, in-memory implementation of {@link GenericRepository}.
 *
 * <p>Subclasses supply two strategies via the constructor:</p>
 * <ol>
 *   <li><b>keyExtractor</b> – extracts the entity's ID (e.g. {@code Employee::getId}).</li>
 *   <li><b>notFoundFactory</b> – constructs the right domain exception for a missing entity.</li>
 * </ol>
 *
 * <h2>Internal data structures</h2>
 * <ul>
 *   <li>{@link HashMap}{@code <ID, T>}  – O(1) look-up by ID</li>
 *   <li>{@link ArrayList}{@code <T>}    – preserves insertion order for listing</li>
 * </ul>
 *
 * <h2>SOLID principles applied</h2>
 * <ul>
 *   <li><b>O – Open/Closed</b>: This class is <em>closed</em> for modification —
 *       all CRUD logic lives here and is never changed per entity type.
 *       It is <em>open</em> for extension: concrete subclasses add entity-specific
 *       query methods (e.g. {@code findByDepartmentId}) without touching this code.</li>
 *   <li><b>L – Liskov Substitution</b>: {@code InMemoryEmployeeRepository} and
 *       {@code InMemoryDepartmentRepository} extend this class and are fully
 *       substitutable for {@link com.srikanth.javareskill.repository.GenericRepository}
 *       anywhere in the codebase.</li>
 * </ul>
 *
 * @param <T>  entity type
 * @param <ID> identifier type — must implement {@code equals} / {@code hashCode}
 */
public abstract class InMemoryRepository<T, ID> implements GenericRepository<T, ID> {

    private final Map<ID, T> idIndex = new HashMap<>();
    private final List<T> entityList = new ArrayList<>();

    private final Function<T, ID> keyExtractor;
    private final Function<ID, RuntimeException> notFoundFactory;

    protected InMemoryRepository(Function<T, ID> keyExtractor,
                                  Function<ID, RuntimeException> notFoundFactory) {
        this.keyExtractor    = Objects.requireNonNull(keyExtractor,    "keyExtractor");
        this.notFoundFactory = Objects.requireNonNull(notFoundFactory, "notFoundFactory");
    }

    @Override
    public void save(T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        ID key = keyExtractor.apply(entity);
        if (idIndex.containsKey(key)) {
            throw new IllegalArgumentException("Entity with ID '" + key + "' already exists");
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
        entityList.set(listIndexOf(key), entity);
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

    /**
     * Read-only view of the internal index for use by subclass query methods.
     */
    protected Map<ID, T> indexView() {
        return Collections.unmodifiableMap(idIndex);
    }

    private int listIndexOf(ID key) {
        for (int i = 0; i < entityList.size(); i++) {
            if (keyExtractor.apply(entityList.get(i)).equals(key)) {
                return i;
            }
        }
        throw new IllegalStateException("Repository in inconsistent state for key: " + key);
    }
}

