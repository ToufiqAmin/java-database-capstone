package com.project.back_end.repo;

import com.project.back_end.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for the Patient entity.
 * Provides basic CRUD operations and custom methods for finding patients 
 * primarily using email and phone number for authentication and lookup.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long>{
    
    // 1. Find a patient by email (Spring Data derived query)
    Patient findByEmail(String email);

    // 2. Find a patient by email OR phone (Spring Data derived query for OR condition)
    Patient findByEmailOrPhone(String email, String phone);
}
