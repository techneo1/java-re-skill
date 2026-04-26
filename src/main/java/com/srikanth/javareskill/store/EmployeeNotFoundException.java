package com.srikanth.javareskill.store;

/**
 * @deprecated Use {@link com.srikanth.javareskill.exception.EmployeeNotFoundException} instead.
 *             This class is retained only for binary compatibility and will be removed in a
 *             future release.
 */
@Deprecated(forRemoval = true)
public class EmployeeNotFoundException
        extends com.srikanth.javareskill.exception.EmployeeNotFoundException {

    public EmployeeNotFoundException(String id) {
        super(id);
    }
}

