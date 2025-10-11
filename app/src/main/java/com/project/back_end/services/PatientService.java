package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class to manage operations related to patients, including creation, 
 * authentication, profile retrieval, and filtering of their appointments.
 * NOTE: The methods below return raw data (Patient or List<AppointmentDTO>) 
 * instead of ResponseEntity, which is handled by the Controller layer.
 * * FIX: Corrected method call from extractIdentifierFromToken() back to extractEmailFromToken()
 * on tokenService to match the public method signature in TokenService.java.
 */
@SuppressWarnings({"unused", "RedundantSuppression"})
@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    // Constructor Injection for Dependencies
    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    /**
     * Helper to convert an Appointment entity to its DTO representation.
     */
    private AppointmentDTO convertToDTO(Appointment appointment) {
        Doctor doctor = appointment.getDoctor();
        Patient patient = appointment.getPatient();

        // Check for null to prevent potential LazyInitializationException outside a transaction
        // Since this method is called within a @Transactional method, this check primarily serves as a safeguard.
        // If called within a transactional boundary, this indicates a deeper data integrity issue.
        if (doctor == null || patient == null) {
            throw new IllegalStateException("Appointment entities (Doctor/Patient) were not properly loaded.");
        }

        return new AppointmentDTO(
                appointment.getId(),
                doctor.getId(),
                doctor.getName(),
                patient.getId(),
                patient.getName(),
                patient.getEmail(),
                patient.getPhone(),
                patient.getAddress(),
                appointment.getAppointmentTime(),
                appointment.getStatus()
        );
    }

    // 1. Saves a new patient to the database
    public int createPatient(Patient patient) {
        try {
            patientRepository.save(patient);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Failure due to exception
        }
    }

    /**
     * 2. Retrieves a list of appointments for a specific patient.
     * @param id The patient's ID.
     * @param token The JWT token containing the email.
     * @return A list of AppointmentDTOs or an empty list on error/unauthorized access.
     */
    @Transactional
    public List<AppointmentDTO> getPatientAppointment(Long id, String token) {
        try {
            // FIX: Using extractEmailFromToken() to match TokenService's public method signature
            String emailFromToken = tokenService.extractEmailFromToken(token);
            Patient authenticatedPatient = patientRepository.findByEmail(emailFromToken);

            // Authorization check: Ensure the authenticated user matches the requested patient ID
            if (authenticatedPatient == null || !authenticatedPatient.getId().equals(id)) {
                System.err.println("Unauthorized access attempt for patient ID: " + id + " with token email: " + emailFromToken);
                return Collections.emptyList();
            }

            // Fetch appointments if authorized
            // FIX: Changed findByPatientId to findByPatient_Id to match JPA convention
            List<Appointment> appointments = appointmentRepository.findByPatient_Id(id);
            return appointments.stream().map(this::convertToDTO).collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList(); // Return empty list on internal server error
        }
    }

    /**
     * 3. Filters appointments by condition ("past" or "future") for a specific patient.
     * @param condition The condition to filter by ("past" or "future").
     * @param id The patient’s ID.
     * @return The filtered appointments or an empty list.
     */
    @Transactional
    public List<AppointmentDTO> filterByCondition(String condition, Long id) {
        try {
            // Mapping condition string to an integer status (0=Future/Upcoming, 1=Past/Completed/Done)
            int status = -1;
            if (condition.equalsIgnoreCase("past")) {
                status = 1;
            } else if (condition.equalsIgnoreCase("future")) {
                status = 0;
            }

            if (status == -1) {
                System.err.println("Invalid condition passed to filterByCondition: " + condition);
                return Collections.emptyList(); // Return empty list on bad request
            }

            // Fetch appointments based on patient ID and status
            List<Appointment> list = appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status);
            return list.stream().map(this::convertToDTO).collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 4. Filters the patient's appointments by doctor's name.
     * @param name The name of the doctor.
     * @param patientId The ID of the patient.
     * @return The filtered appointments or an empty list.
     */
    @Transactional
    public List<AppointmentDTO> filterByDoctor(String name, Long patientId) {
        try {
            // Fetch appointments based on patient ID and doctor name (partial match assumed)
            List<Appointment> list = appointmentRepository.filterByDoctorNameAndPatientId(name, patientId);
            return list.stream().map(this::convertToDTO).collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 5. Filters the patient's appointments by doctor's name and appointment condition (past or future).
     * @param condition The condition to filter by ("past" or "future").
     * @param name The name of the doctor.
     * @param patientId The ID of the patient.
     * @return The filtered appointments or an empty list.
     */
    @Transactional
    public List<AppointmentDTO> filterByDoctorAndCondition(String condition, String name, long patientId) {
        try {
            // Mapping condition string to an integer status
            int status = -1;
            if (condition.equalsIgnoreCase("past")) {
                status = 1;
            } else if (condition.equalsIgnoreCase("future")) {
                status = 0;
            }

            if (status == -1) {
                System.err.println("Invalid condition passed to filterByDoctorAndCondition: " + condition);
                return Collections.emptyList();
            }

            // Fetch appointments based on doctor name, patient ID, and status
            List<Appointment> list = appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(name, patientId, status);
            return list.stream().map(this::convertToDTO).collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 6. Fetches the patient's details based on the provided JWT token.
     * @param token The JWT token containing the email.
     * @return The patient object or null on failure/not found.
     */
    public Patient getPatientDetails(String token) {
        try {
            // FIX: Using extractEmailFromToken() to match TokenService's public method signature
            String email = tokenService.extractEmailFromToken(token);
            Patient patient = patientRepository.findByEmail(email);

            if (patient == null) {
                System.err.println("Patient not found for email extracted from token: " + email);
            }
            
            return patient;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Return null on invalid token or internal server error
        }
    }
}
