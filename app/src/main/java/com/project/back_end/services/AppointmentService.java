package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class to handle appointment-related business logic, 
 * including scheduling, modification, retrieval, and filtering.
 * * * FIXES: 
 * 1. Corrected method call from extractIdentifierFromToken() back to extractEmailFromToken() 
 * on tokenService to match the public method signature in TokenService.java.
 * 2. Renamed scheduleAppointment to bookAppointment to match controller expectation.
 * 3. Updated convertToDTO to match the full 10-argument AppointmentDTO constructor.
 * 4. Added changeAppointmentStatus method to satisfy controller dependencies.
 * 5. Added authorization logic to doctor appointment retrieval methods.
 */
@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final TokenService tokenService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository,
                              TokenService tokenService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.tokenService = tokenService;
    }

    /**
     * Schedules a new appointment.
     * Renamed from scheduleAppointment to match controller expectation.
     * @param appointment The appointment entity to save.
     * @param token The JWT token to verify user (patient) identity.
     * @return 1 for success, -1 for slot conflict, -2 for doctor not found, -3 for patient not found, 0 for error.
     */
    @Transactional
    public int bookAppointment(Appointment appointment, String token) {
        // FIX: Using extractEmailFromToken
        String patientEmail = tokenService.extractEmailFromToken(token);
        Patient patient = patientRepository.findByEmail(patientEmail);

        if (patient == null) return -3; // Patient not found/invalid token

        // Ensure Doctor exists and set it on the appointment
        Optional<Doctor> doctorOpt = doctorRepository.findById(appointment.getDoctor().getId());
        if (doctorOpt.isEmpty()) return -2; // Doctor not found

        // Check for double booking (same doctor, same time)
        LocalDateTime appointmentTime = appointment.getAppointmentTime();
        List<Appointment> existingAppointments = appointmentRepository
                .findByDoctor_IdAndAppointmentTimeBetween(
                        appointment.getDoctor().getId(),
                        appointmentTime.minusMinutes(1),
                        appointmentTime.plusMinutes(1)
                );

        if (!existingAppointments.isEmpty()) return -1; // Slot Conflict

        try {
            appointment.setPatient(patient);
            appointment.setDoctor(doctorOpt.get());
            appointment.setStatus(1); // Set to scheduled (or similar initial status)
            appointmentRepository.save(appointment);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Internal error
        }
    }

    /**
     * Updates an existing appointment. Only the doctor or patient can update their own appointment.
     * @param updatedAppointment The appointment entity with updated details.
     * @param token The JWT token for authorization.
     * @return 1 for success, -1 for not found, -2 for unauthorized, 0 for error.
     */
    @Transactional
    public int updateAppointment(Appointment updatedAppointment, String token) {
        Optional<Appointment> existingAppointmentOpt = appointmentRepository.findById(updatedAppointment.getId());
        if (existingAppointmentOpt.isEmpty()) return -1; // Not found

        Appointment existingAppointment = existingAppointmentOpt.get();
        // FIX: Using extractEmailFromToken
        String identifier = tokenService.extractEmailFromToken(token);

        // Authorization check: Is the user the patient or the doctor for this appointment?
        boolean isPatient = existingAppointment.getPatient().getEmail().equals(identifier);
        boolean isDoctor = existingAppointment.getDoctor().getEmail().equals(identifier);

        if (!isPatient && !isDoctor) return -2; // Unauthorized

        try {
            // Apply updates, but typically limit what can be changed (e.g., only time/status)
            // For simplicity, we save the entire updated entity assuming ID preservation
            existingAppointment.setAppointmentTime(updatedAppointment.getAppointmentTime());
            existingAppointment.setStatus(updatedAppointment.getStatus());
            // NOTE: This line requires Appointment.java to have getReason() and setReason()
            existingAppointment.setReason(updatedAppointment.getReason()); 
            
            appointmentRepository.save(existingAppointment);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Internal error
        }
    }

    /**
     * Cancels an appointment by ID.
     * @param appointmentId The ID of the appointment to cancel.
     * @param token The JWT token for authorization.
     * @return 1 for success, -1 for not found, -2 for unauthorized, 0 for error.
     */
    @Transactional
    public int cancelAppointment(long appointmentId, String token) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) return -1; // Not found

        Appointment appointment = appointmentOpt.get();
        // FIX: Using extractEmailFromToken
        String identifier = tokenService.extractEmailFromToken(token);

        // Authorization check: Is the user the patient or the doctor?
        boolean isPatient = appointment.getPatient().getEmail().equals(identifier);
        boolean isDoctor = appointment.getDoctor().getEmail().equals(identifier);

        if (!isPatient && !isDoctor) return -2; // Unauthorized

        try {
            // Use the updateStatus repository method
            appointmentRepository.updateStatus(0, appointmentId); // Status 0 often means 'Cancelled'
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Internal error
        }
    }

    /**
     * Retrieves all appointments for a specific doctor on a given date.
     * Added token for authorization check to satisfy the controller's call.
     * @param doctorId The ID of the doctor.
     * @param date The date of appointments.
     * @param token The JWT token for authorization.
     * @return List of appointments or empty list if unauthorized.
     */
    @Transactional
    public List<Appointment> getAppointmentsForDoctorOnDate(Long doctorId, LocalDate date, String token) {
        String doctorEmail = tokenService.extractEmailFromToken(token);
        // FIX: Use Optional.ofNullable to correctly handle the result of findByEmail
        Optional<Doctor> authenticatedDoctor = Optional.ofNullable(doctorRepository.findByEmail(doctorEmail));

        if (authenticatedDoctor.isEmpty() || !authenticatedDoctor.get().getId().equals(doctorId)) {
            System.err.println("Unauthorized access attempt by non-matching doctor to appointments.");
            return Collections.emptyList();
        }
        
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // FIX: Using correct repository method name from the AppointmentRepository
        return appointmentRepository.findByDoctor_IdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay);
    }
    
    /**
     * Filters appointments for a doctor by patient name and date range.
     * @param doctorId The ID of the doctor.
     * @param patientName Partial patient name to filter by.
     * @param date The date to filter by.
     * @return List of filtered appointments.
     */
    @Transactional
    public List<Appointment> filterAppointmentsForDoctor(Long doctorId, String patientName, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        if (patientName != null && !patientName.isEmpty()) {
            // FIX: Using correct repository method name for filtering by patient name
            return appointmentRepository.findByDoctor_IdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                    doctorId, patientName, startOfDay, endOfDay
            );
        } else {
            // FIX: Using correct repository method name for filtering by date only
            return appointmentRepository.findByDoctor_IdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay);
        }
    }

    /**
     * Changes the status of an appointment.
     * Added to satisfy a missing method call from PrescriptionController.
     * @param appointmentId The ID of the appointment.
     * @param status The new status value (e.g., 0=Cancelled, 1=Scheduled, 2=Completed).
     * @param token The JWT token for authorization.
     * @return 1 for success, -1 for not found, -2 for unauthorized, 0 for error.
     */
    @Transactional
    public int changeAppointmentStatus(Long appointmentId, int status, String token) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) return -1; // Not found

        Appointment appointment = appointmentOpt.get();
        String identifier = tokenService.extractEmailFromToken(token);

        // Authorization check: Is the user the patient or the doctor?
        boolean isPatient = appointment.getPatient().getEmail().equals(identifier);
        boolean isDoctor = appointment.getDoctor().getEmail().equals(identifier);

        if (!isPatient && !isDoctor) return -2; // Unauthorized

        try {
            appointmentRepository.updateStatus(status, appointmentId);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Internal error
        }
    }

    /**
     * Converts an Appointment entity to its DTO representation using the full 10-argument constructor.
     * FIX: Updated to match the required AppointmentDTO constructor.
     */
    private AppointmentDTO convertToDTO(Appointment appointment) {
        if (appointment == null) return null;
        Doctor doctor = appointment.getDoctor();
        Patient patient = appointment.getPatient();
        
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

    /**
     * Retrieves all appointments for a doctor on a specific date, converted to DTOs.
     * Updated to pass the token to the underlying method.
     * @param doctorId ID of the doctor.
     * @param date Date of appointments.
     * @param token The JWT token for authorization.
     * @return List of AppointmentDTOs.
     */
    public List<AppointmentDTO> getAppointmentsForDoctorOnDateDTO(Long doctorId, LocalDate date, String token) {
        return getAppointmentsForDoctorOnDate(doctorId, date, token).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
