package com.project.back_end.repo;

import com.project.back_end.models.Prescription;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB Repository for the Prescription Document.
 * Extends MongoRepository to provide CRUD operations for the prescriptions collection.
 */
@Repository
public interface PrescriptionRepository extends MongoRepository<Prescription, String>{
    
    // Custom query method to find prescriptions by appointment ID
    List<Prescription> findByAppointmentId(Long appointmentId);
}
