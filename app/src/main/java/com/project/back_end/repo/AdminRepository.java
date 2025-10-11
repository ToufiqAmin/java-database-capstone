package com.project.back_end.repo;

import com.project.back_end.models.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for the Admin entity.
 * Extends JpaRepository to inherit standard CRUD operations.
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    // 1. Extend JpaRepository: Done by 'extends JpaRepository<Admin, Long>'.

    // 2. Custom Query Method: findByUsername
    /**
     * Retrieves an Admin entity based on the provided username.
     * Spring Data JPA automatically derives the query from the method name.
     * * @param username The username of the admin to find.
     * @return The Admin entity, or null if not found.
     */
    Admin findByUsername(String username);
}
