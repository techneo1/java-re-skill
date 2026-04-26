package com.srikanth.javareskill.service.impl;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.repository.DepartmentRepository;
import com.srikanth.javareskill.service.DepartmentService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link DepartmentService}.
 *
 * <p>Delegates persistence to a {@link DepartmentRepository}.</p>
 */
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository repository;

    public DepartmentServiceImpl(DepartmentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public void create(Department department) {
        Objects.requireNonNull(department, "department must not be null");
        repository.save(department);
    }

    @Override
    public Optional<Department> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public List<Department> findAll() {
        return repository.findAll();
    }

    @Override
    public void update(Department department) {
        repository.update(department);
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }

    @Override
    public List<Department> findByLocation(String location) {
        return repository.findByLocation(location);
    }

    @Override
    public Optional<Department> findByName(String name) {
        return repository.findByName(name);
    }

    @Override
    public int count() {
        return repository.count();
    }
}

