package com.srikanth.javareskill.repository;
import java.util.function.Function;
/**
 * @deprecated Use {@link com.srikanth.javareskill.repository.inmemory.InMemoryRepository} instead.
 *             This alias is retained for binary compatibility only.
 */
@Deprecated(forRemoval = true)
public abstract class InMemoryRepository<T, ID>
        extends com.srikanth.javareskill.repository.inmemory.InMemoryRepository<T, ID> {
    protected InMemoryRepository(Function<T, ID> keyExtractor,
                                  Function<ID, RuntimeException> notFoundFactory) {
        super(keyExtractor, notFoundFactory);
    }
}
