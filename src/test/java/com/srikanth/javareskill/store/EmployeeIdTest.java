package com.srikanth.javareskill.store;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link EmployeeId} fulfils the HashMap key contract:
 *
 * <ol>
 *   <li>Correct {@code equals} / {@code hashCode} semantics</li>
 *   <li>Consistent retrieval from a real {@link HashMap}</li>
 *   <li>Case-insensitive normalisation</li>
 *   <li>Immutability & validation</li>
 * </ol>
 */
class EmployeeIdTest {

    // =========================================================================
    // equals contract
    // =========================================================================

    @Test
    void equals_sameValue_returnsTrue() {
        EmployeeId a = new EmployeeId("E001");
        EmployeeId b = new EmployeeId("E001");
        assertEquals(a, b);
    }

    @Test
    void equals_differentValue_returnsFalse() {
        assertNotEquals(new EmployeeId("E001"), new EmployeeId("E002"));
    }

    @Test
    void equals_reflexive() {
        EmployeeId id = new EmployeeId("E001");
        assertEquals(id, id);
    }

    @Test
    void equals_symmetric() {
        EmployeeId a = new EmployeeId("E001");
        EmployeeId b = new EmployeeId("E001");
        assertEquals(a, b);
        assertEquals(b, a);
    }

    @Test
    void equals_transitive() {
        EmployeeId a = new EmployeeId("E001");
        EmployeeId b = new EmployeeId("E001");
        EmployeeId c = new EmployeeId("E001");
        assertEquals(a, b);
        assertEquals(b, c);
        assertEquals(a, c);
    }

    @Test
    void equals_null_returnsFalse() {
        assertNotEquals(new EmployeeId("E001"), null);
    }

    @Test
    void equals_differentType_returnsFalse() {
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals(new EmployeeId("E001"), "E001");
    }

    // =========================================================================
    // hashCode contract
    // =========================================================================

    @Test
    void hashCode_equalObjects_haveSameHashCode() {
        EmployeeId a = new EmployeeId("E001");
        EmployeeId b = new EmployeeId("E001");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void hashCode_consistent_acrossMultipleCalls() {
        EmployeeId id = new EmployeeId("E001");
        int first = id.hashCode();
        // Call multiple times — must be stable (object is immutable)
        assertEquals(first, id.hashCode());
        assertEquals(first, id.hashCode());
    }

    // =========================================================================
    // Case-insensitive normalisation
    // =========================================================================

    @Test
    void equals_caseInsensitive_lowerAndUpperEqual() {
        assertEquals(new EmployeeId("e001"), new EmployeeId("E001"));
    }

    @Test
    void equals_caseInsensitive_mixedCase() {
        assertEquals(new EmployeeId("e001"), new EmployeeId("E001"));
        assertEquals(new EmployeeId("E001"), new EmployeeId("e001"));
        assertEquals(new EmployeeId("E001"), new EmployeeId("E001"));
    }

    @Test
    void hashCode_caseInsensitive_sameHash() {
        assertEquals(
                new EmployeeId("e001").hashCode(),
                new EmployeeId("E001").hashCode());
    }

    // =========================================================================
    // HashMap key behaviour — the core goal of this exercise
    // =========================================================================

    @Test
    void hashMap_lookupByNewEqualKey_returnsValue() {
        // Demonstrates that two distinct EmployeeId objects with the same value
        // resolve to the same HashMap entry — proving equals+hashCode are correct.
        Map<EmployeeId, String> map = new HashMap<>();
        EmployeeId insertKey = new EmployeeId("E001");
        EmployeeId lookupKey = new EmployeeId("E001"); // different object, same value

        map.put(insertKey, "Alice");

        // lookupKey is NOT the same reference as insertKey
        assertNotSame(insertKey, lookupKey);
        // But the map must still find the entry
        assertEquals("Alice", map.get(lookupKey));
    }

    @Test
    void hashMap_lookupByCaseInsensitiveKey_returnsValue() {
        Map<EmployeeId, String> map = new HashMap<>();
        map.put(new EmployeeId("E001"), "Alice");

        // Lookup with lower-case variant must hit the same bucket entry
        assertEquals("Alice", map.get(new EmployeeId("e001")));
    }

    @Test
    void hashMap_distinctKeys_storedSeparately() {
        Map<EmployeeId, String> map = new HashMap<>();
        map.put(new EmployeeId("E001"), "Alice");
        map.put(new EmployeeId("E002"), "Bob");

        assertEquals(2, map.size());
        assertEquals("Alice", map.get(new EmployeeId("E001")));
        assertEquals("Bob",   map.get(new EmployeeId("E002")));
    }

    @Test
    void hashMap_putTwiceWithEqualKey_overwritesValue() {
        Map<EmployeeId, String> map = new HashMap<>();
        map.put(new EmployeeId("E001"), "Alice");
        map.put(new EmployeeId("E001"), "Alice Updated");

        assertEquals(1, map.size());
        assertEquals("Alice Updated", map.get(new EmployeeId("E001")));
    }

    @Test
    void hashMap_containsKey_usesEquals() {
        Map<EmployeeId, String> map = new HashMap<>();
        map.put(new EmployeeId("E001"), "Alice");

        assertTrue(map.containsKey(new EmployeeId("E001")));
        assertTrue(map.containsKey(new EmployeeId("e001"))); // case-insensitive
        assertFalse(map.containsKey(new EmployeeId("E002")));
    }

    @Test
    void hashMap_remove_usesEquals() {
        Map<EmployeeId, String> map = new HashMap<>();
        map.put(new EmployeeId("E001"), "Alice");

        map.remove(new EmployeeId("e001")); // different object, lower-case
        assertFalse(map.containsKey(new EmployeeId("E001")));
        assertTrue(map.isEmpty());
    }

    // =========================================================================
    // Validation
    // =========================================================================

    @Test
    void constructor_nullValue_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new EmployeeId(null));
    }

    @Test
    void constructor_blankValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new EmployeeId("   "));
        assertThrows(IllegalArgumentException.class, () -> new EmployeeId(""));
    }

    @Test
    void getValue_returnsNormalisedUpperCase() {
        assertEquals("E001", new EmployeeId("e001").getValue());
        assertEquals("E001", new EmployeeId("E001").getValue());
    }

    @Test
    void toString_containsValue() {
        assertTrue(new EmployeeId("E001").toString().contains("E001"));
    }
}

