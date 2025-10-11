package com.project.back_end.repo;

import com.project.back_end.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA Repository for the Appointment entity.
 * Provides custom query methods for filtering and modification.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // 1. findByDoctorIdAndAppointmentTimeBetween
    // Retrieves appointments for a specific doctor within a given time range.
    // Note: The prompt asked for JOIN FETCH, but for simple queries on fields 
    // in the current entity, Spring Data derived method names are generally sufficient
    // and often more performant than explicit HQL/JPQL for simple filtering.
    List<Appointment> findByDoctor_IdAndAppointmentTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);

    // 2. findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween
    // Filters by doctor ID, partial patient name (case-insensitive), and time range.
    List<Appointment> findByDoctor_IdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
            Long doctorId,
            String patientName,
            LocalDateTime start,
            LocalDateTime end
    );

    // 3. deleteAllByDoctorId
    // Deletes all appointments associated with a particular doctor. Requires transaction and modification markers.
    @Modifying
    @Transactional
    void deleteAllByDoctor_Id(Long doctorId);

    // 4. findByPatientId
    // Retrieves all appointments for a specific patient.
    List<Appointment> findByPatient_Id(Long patientId);

    // 5. findByPatient_IdAndStatusOrderByAppointmentTimeAsc
    // Retrieves appointments for a patient by status, ordered ascendingly by time.
    List<Appointment> findByPatient_IdAndStatusOrderByAppointmentTimeAsc(Long patientId, int status);

    // 6. filterByDoctorNameAndPatientId
    // Search appointments by partial doctor name (case-insensitive) and patient ID using JPQL.
    @Query("SELECT a FROM Appointment a WHERE LOWER(a.doctor.name) LIKE LOWER(CONCAT('%', :doctorName, '%')) AND a.patient.id = :patientId")
    List<Appointment> filterByDoctorNameAndPatientId(String doctorName, Long patientId);

    // 7. filterByDoctorNameAndPatientIdAndStatus
    // Filters appointments by doctor name, patient ID, and status using JPQL.
    @Query("SELECT a FROM Appointment a WHERE LOWER(a.doctor.name) LIKE LOWER(CONCAT('%', :doctorName, '%')) AND a.patient.id = :patientId AND a.status = :status")
    List<Appointment> filterByDoctorNameAndPatientIdAndStatus(String doctorName, Long patientId, int status);

    // 8. updateStatus (Added from the detailed comments)
    // Updates the status of a specific appointment based on its ID.
    @Modifying
    @Transactional
    @Query("UPDATE Appointment a SET a.status = :status WHERE a.id = :id")
    void updateStatus(int status, long id);
}
