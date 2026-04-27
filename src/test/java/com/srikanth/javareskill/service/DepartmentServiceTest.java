package com.srikanth.javareskill.service;

import com.srikanth.javareskill.domain.Department;
import com.srikanth.javareskill.exception.DepartmentNotFoundException;
import com.srikanth.javareskill.repository.DepartmentRepository;
import com.srikanth.javareskill.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DepartmentServiceImpl} using Mockito to isolate
 * the service from its repository dependency.
 *
 * <p>Covers every method declared in {@link DepartmentService} to ensure
 * ≥80 % service-layer code coverage.</p>
 *
 * <h2>Mockito features used</h2>
 * <ul>
 *   <li>{@code @Mock} / {@code @InjectMocks} – constructor injection</li>
 *   <li>{@code @Captor} – argument inspection beyond simple equality</li>
 *   <li>{@code verify()} / {@code verifyNoMoreInteractions()} – interaction assertions</li>
 *   <li>{@code doThrow()} – exception-propagation scenarios</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentServiceImpl")
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository repository;

    @InjectMocks
    private DepartmentServiceImpl service;

    @Captor
    private ArgumentCaptor<Department> departmentCaptor;

    @Captor
    private ArgumentCaptor<String> idCaptor;

    // -------------------------------------------------------------------------
    // Test-data helpers
    // -------------------------------------------------------------------------

    private static Department dept(String id, String name, String location) {
        return Department.builder().id(id).name(name).location(location).build();
    }

    private static final Department ENGINEERING = dept("D001", "Engineering", "New York");
    private static final Department HR          = dept("D002", "HR",          "Boston");
    private static final Department FINANCE     = dept("D003", "Finance",     "New York");

    // =========================================================================
    // Constructor
    // =========================================================================

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("null repository throws NullPointerException")
        void nullRepository_throwsNpe() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new DepartmentServiceImpl(null))
                    .withMessageContaining("repository must not be null");
        }
    }

    // =========================================================================
    // create()
    // =========================================================================

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("delegates save to repository")
        void create_delegatesToRepository() {
            service.create(ENGINEERING);

            verify(repository).save(ENGINEERING);
        }

        @Test
        @DisplayName("captures and verifies exact Department forwarded to repository.save()")
        void create_capturesDepartmentPassedToRepository() {
            service.create(ENGINEERING);

            verify(repository).save(departmentCaptor.capture());
            Department saved = departmentCaptor.getValue();

            assertThat(saved.getId())       .isEqualTo("D001");
            assertThat(saved.getName())     .isEqualTo("Engineering");
            assertThat(saved.getLocation()) .isEqualTo("New York");
        }

        @Test
        @DisplayName("null department throws NullPointerException before touching repository")
        void create_nullDepartment_throwsNpe() {
            assertThatNullPointerException()
                    .isThrownBy(() -> service.create(null))
                    .withMessageContaining("department must not be null");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("repository.save() called exactly once; no other interactions")
        void create_onlySaveCalled() {
            service.create(ENGINEERING);

            verify(repository, times(1)).save(any());
            verifyNoMoreInteractions(repository);
        }
    }

    // =========================================================================
    // findById()
    // =========================================================================

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("returns department wrapped in Optional when found")
        void findById_found_returnsOptional() {
            when(repository.findById("D001")).thenReturn(Optional.of(ENGINEERING));

            assertThat(service.findById("D001")).contains(ENGINEERING);
        }

        @Test
        @DisplayName("returns empty Optional when not found")
        void findById_notFound_returnsEmpty() {
            when(repository.findById("GHOST")).thenReturn(Optional.empty());

            assertThat(service.findById("GHOST")).isEmpty();
        }

        @Test
        @DisplayName("captures and verifies exact id forwarded to repository")
        void findById_capturesId() {
            when(repository.findById(any())).thenReturn(Optional.of(ENGINEERING));

            service.findById("D001");

            verify(repository).findById(idCaptor.capture());
            assertThat(idCaptor.getValue()).isEqualTo("D001");
        }

        @Test
        @DisplayName("delegates to repository exactly once")
        void findById_delegatesToRepositoryOnce() {
            when(repository.findById("D001")).thenReturn(Optional.of(ENGINEERING));

            service.findById("D001");

            verify(repository, times(1)).findById("D001");
        }
    }

    // =========================================================================
    // findAll()
    // =========================================================================

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("returns all departments from repository")
        void findAll_returnsRepositoryResult() {
            when(repository.findAll()).thenReturn(List.of(ENGINEERING, HR, FINANCE));

            assertThat(service.findAll()).containsExactly(ENGINEERING, HR, FINANCE);
        }

        @Test
        @DisplayName("returns empty list when repository is empty")
        void findAll_empty_returnsEmptyList() {
            when(repository.findAll()).thenReturn(Collections.emptyList());

            assertThat(service.findAll()).isEmpty();
        }

        @Test
        @DisplayName("repository.findAll() called exactly once; no other interactions")
        void findAll_onlyFindAllCalled() {
            when(repository.findAll()).thenReturn(List.of(ENGINEERING));

            service.findAll();

            verify(repository, times(1)).findAll();
            verifyNoMoreInteractions(repository);
        }
    }

    // =========================================================================
    // update()
    // =========================================================================

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("delegates update to repository")
        void update_delegatesToRepository() {
            service.update(ENGINEERING);

            verify(repository).update(ENGINEERING);
        }

        @Test
        @DisplayName("captures and verifies exact Department forwarded to repository.update()")
        void update_capturesDepartmentPassedToRepository() {
            Department relocated = dept("D001", "Engineering", "San Francisco");

            service.update(relocated);

            verify(repository).update(departmentCaptor.capture());
            assertThat(departmentCaptor.getValue().getLocation()).isEqualTo("San Francisco");
            assertThat(departmentCaptor.getValue().getId()).isEqualTo("D001");
        }

        @Test
        @DisplayName("propagates DepartmentNotFoundException from repository")
        void update_notFound_propagatesException() {
            doThrow(new DepartmentNotFoundException("GHOST"))
                    .when(repository).update(any());

            assertThatThrownBy(() -> service.update(ENGINEERING))
                    .isInstanceOf(DepartmentNotFoundException.class);
        }

        @Test
        @DisplayName("repository.update() called exactly once; no save/delete side-effects")
        void update_onlyUpdateCalled() {
            service.update(ENGINEERING);

            verify(repository, times(1)).update(any());
            verify(repository, never()).save(any());
            verify(repository, never()).deleteById(any());
        }
    }

    // =========================================================================
    // delete()
    // =========================================================================

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("delegates deleteById to repository")
        void delete_delegatesToRepository() {
            service.delete("D001");

            verify(repository).deleteById("D001");
        }

        @Test
        @DisplayName("captures and verifies exact id forwarded to repository.deleteById()")
        void delete_capturesIdPassedToRepository() {
            service.delete("D002");

            verify(repository).deleteById(idCaptor.capture());
            assertThat(idCaptor.getValue()).isEqualTo("D002");
        }

        @Test
        @DisplayName("propagates DepartmentNotFoundException from repository")
        void delete_notFound_propagatesException() {
            doThrow(new DepartmentNotFoundException("GHOST"))
                    .when(repository).deleteById("GHOST");

            assertThatThrownBy(() -> service.delete("GHOST"))
                    .isInstanceOf(DepartmentNotFoundException.class);
        }

        @Test
        @DisplayName("repository.deleteById() called exactly once; no save/update side-effects")
        void delete_onlyDeleteCalled() {
            service.delete("D001");

            verify(repository, times(1)).deleteById(any());
            verify(repository, never()).save(any());
            verify(repository, never()).update(any());
        }
    }

    // =========================================================================
    // findByLocation()
    // =========================================================================

    @Nested
    @DisplayName("findByLocation()")
    class FindByLocationTests {

        @Test
        @DisplayName("returns departments for matching location")
        void findByLocation_returnsMatchingDepartments() {
            when(repository.findByLocation("New York")).thenReturn(List.of(ENGINEERING, FINANCE));

            assertThat(service.findByLocation("New York"))
                    .hasSize(2)
                    .containsExactly(ENGINEERING, FINANCE);
        }

        @Test
        @DisplayName("returns empty list when no departments at location")
        void findByLocation_noMatch_returnsEmpty() {
            when(repository.findByLocation("Chicago")).thenReturn(Collections.emptyList());

            assertThat(service.findByLocation("Chicago")).isEmpty();
        }

        @Test
        @DisplayName("captures and verifies exact location string forwarded to repository")
        void findByLocation_capturesLocation() {
            service.findByLocation("Austin");

            verify(repository).findByLocation(idCaptor.capture());
            assertThat(idCaptor.getValue())
                    .as("location must be forwarded unchanged")
                    .isEqualTo("Austin");
        }

        @Test
        @DisplayName("delegates to repository exactly once")
        void findByLocation_delegatesToRepositoryOnce() {
            service.findByLocation("New York");

            verify(repository, times(1)).findByLocation("New York");
        }
    }

    // =========================================================================
    // findByName()
    // =========================================================================

    @Nested
    @DisplayName("findByName()")
    class FindByNameTests {

        @Test
        @DisplayName("returns department wrapped in Optional when found")
        void findByName_found_returnsOptional() {
            when(repository.findByName("Engineering")).thenReturn(Optional.of(ENGINEERING));

            assertThat(service.findByName("Engineering")).contains(ENGINEERING);
        }

        @Test
        @DisplayName("returns empty Optional when not found")
        void findByName_notFound_returnsEmpty() {
            when(repository.findByName("Legal")).thenReturn(Optional.empty());

            assertThat(service.findByName("Legal")).isEmpty();
        }

        @Test
        @DisplayName("captures and verifies exact name string forwarded to repository")
        void findByName_capturesName() {
            service.findByName("HR");

            verify(repository).findByName(idCaptor.capture());
            assertThat(idCaptor.getValue()).isEqualTo("HR");
        }

        @Test
        @DisplayName("delegates to repository exactly once")
        void findByName_delegatesToRepositoryOnce() {
            service.findByName("Finance");

            verify(repository, times(1)).findByName("Finance");
        }
    }

    // =========================================================================
    // count()
    // =========================================================================

    @Nested
    @DisplayName("count()")
    class CountTests {

        @Test
        @DisplayName("returns count from repository")
        void count_returnsRepositoryCount() {
            when(repository.count()).thenReturn(7);

            assertThat(service.count()).isEqualTo(7);
        }

        @Test
        @DisplayName("returns zero when repository is empty")
        void count_empty_returnsZero() {
            when(repository.count()).thenReturn(0);

            assertThat(service.count()).isZero();
        }

        @Test
        @DisplayName("delegates to repository.count() exactly once; no other interactions")
        void count_delegatesToRepositoryOnce() {
            service.count();

            verify(repository, times(1)).count();
            verifyNoMoreInteractions(repository);
        }
    }
}

