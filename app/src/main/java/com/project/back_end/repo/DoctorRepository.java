package com.project.back_end.repo;

import com.project.back_end.models.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for the Doctor entity.
 * Provides custom query methods for filtering by name, email, and specialty.
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long>{

    // 1. Find doctor by exact email (Spring Data derived query)
    Doctor findByEmail(String email);

    // 2. Find doctors by partial name match using JPQL and CONCAT 
    // to include wildcards, ensuring flexible pattern matching as requested by the hint.
    @Query("SELECT d FROM Doctor d WHERE d.name LIKE CONCAT('%', :name, '%')")
    List<Doctor> findByNameLike(String name);

    // 3. Filter doctors by partial name (ContainingIgnoreCase) and exact specialty (IgnoreCase).
    // This uses Spring Data derived method naming for case-insensitive partial/exact matching.
    List<Doctor> findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(String name, String specialty);

    // 4. Find doctors by specialty, ignoring case (Spring Data derived query)
    List<Doctor> findBySpecialtyIgnoreCase(String specialty);
}
