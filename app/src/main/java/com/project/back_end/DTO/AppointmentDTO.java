package com.project.back_end.DTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Data Transfer Object (DTO) for Appointment information.
 * This class decouples frontend data requirements from the internal database structure
 * and provides derived fields like appointmentDate, appointmentTimeOnly, and endTime.
 */
public class AppointmentDTO {

    // 1. 'id' field
    private final Long id;
    // 2. 'doctorId' field
    private final Long doctorId;
    // 3. 'doctorName' field
    private final String doctorName;
    // 4. 'patientId' field
    private final Long patientId;
    // 5. 'patientName' field
    private final String patientName;
    // 6. 'patientEmail' field
    private final String patientEmail;
    // 7. 'patientPhone' field
    private final String patientPhone;
    // 8. 'patientAddress' field
    private final String patientAddress;
    // 9. 'appointmentTime' field
    private final LocalDateTime appointmentTime;
    // 10. 'status' field
    private final int status;

    // Derived Fields (Calculated in the constructor)
    // 11. 'appointmentDate' field (Derived)
    private final LocalDate appointmentDate;
    // 12. 'appointmentTimeOnly' field (Derived)
    private final LocalTime appointmentTimeOnly;
    // 13. 'endTime' field (Derived)
    private final LocalDateTime endTime;

    /**
     * 14. Constructor: Initializes all core fields and automatically computes 
     * the derived fields (appointmentDate, appointmentTimeOnly, and endTime).
     */
    public AppointmentDTO(
            Long id, Long doctorId, String doctorName, Long patientId, String patientName,
            String patientEmail, String patientPhone, String patientAddress,
            LocalDateTime appointmentTime, int status) {

        // Core field assignment
        this.id = id;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.patientPhone = patientPhone;
        this.patientAddress = patientAddress;
        this.appointmentTime = appointmentTime;
        this.status = status;

        // Custom/Derived field calculation
        if (this.appointmentTime != null) {
            // 11. Extract date
            this.appointmentDate = this.appointmentTime.toLocalDate();
            // 12. Extract time
            this.appointmentTimeOnly = this.appointmentTime.toLocalTime();
            // 13. Calculate end time (1 hour duration)
            this.endTime = this.appointmentTime.plusHours(1);
        } else {
            // Handle null case for safety
            this.appointmentDate = null;
            this.appointmentTimeOnly = null;
            this.endTime = null;
        }
    }

    // --- 15. Standard Getter Methods ---

    public Long getId() {
        return id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientEmail() {
        return patientEmail;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public String getPatientAddress() {
        return patientAddress;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public int getStatus() {
        return status;
    }

    // --- Getters for Derived Fields ---

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public LocalTime getAppointmentTimeOnly() {
        return appointmentTimeOnly;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}