package com.srikanth.javareskill.repository;

import com.srikanth.javareskill.domain.PayrollRecord;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory store for processed {@link PayrollRecord} objects.
 *
 * <p>A separate, lightweight store is used instead of extending the existing
 * {@link InMemoryRepository} because {@code PayrollRecord} has no "update"
 * semantics — records are append-only (written once per payroll run) and
 * queried by month.</p>
 *
 * <p>Registered as a Spring singleton bean in {@code SpringAppConfig}.</p>
 */
public class InMemoryPayrollRepository {

    /**
     * {@link CopyOnWriteArrayList} gives safe concurrent reads (common) without
     * locking, at the cost of slightly slower writes (rare — one batch per month).
     */
    private final List<PayrollRecord> store = new CopyOnWriteArrayList<>();

    /**
     * Appends a processed payroll record to the store.
     *
     * @param record must not be {@code null}
     */
    public void save(PayrollRecord record) {
        store.add(record);
    }

    /**
     * Saves all records in the given list atomically (from the caller's perspective;
     * the underlying list is still appended one-by-one).
     *
     * @param records must not be {@code null}
     */
    public void saveAll(List<PayrollRecord> records) {
        store.addAll(records);
    }

    /**
     * Returns all payroll records whose {@code payrollMonth} falls in the given
     * {@link YearMonth}.
     *
     * <p>Matching is done by comparing year and month only; the day component of
     * {@code PayrollRecord#getPayrollMonth()} is always the 1st (normalised on
     * creation).</p>
     *
     * @param month the year-month to filter by; must not be {@code null}
     * @return unmodifiable list of matching records (may be empty)
     */
    public List<PayrollRecord> findByMonth(YearMonth month) {
        return store.stream()
                .filter(r -> YearMonth.from(r.getPayrollMonth()).equals(month))
                .toList();
    }

    /**
     * Returns a snapshot of every record in the store (all months).
     *
     * @return unmodifiable list
     */
    public List<PayrollRecord> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store));
    }

    /** Returns the total number of stored records across all months. */
    public int count() {
        return store.size();
    }
}

