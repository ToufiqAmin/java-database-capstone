package com.project.back_end.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * JPA Entity representing a scheduled Appointment between a Doctor and a Patient.
 * Includes validation rules and transient methods for derived time values.
 * FIX: Added 'reason' field and corresponding getter/setter to resolve compilation errors
 * in AppointmentService.
 */
@Entity
@Table(name = "appointment")
public class Appointment {

    // 1. Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 2. Doctor Relationship
    @NotNull(message = "Doctor must be assigned")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    // 3. Patient Relationship
    @NotNull(message = "Patient must be assigned")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // 4. Appointment Time (Must be in the future)
    @NotNull(message = "Appointment time must be provided")
    @Future(message = "Appointment time must be in the future")
    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    // 5. Status (0 = scheduled, 1 = completed)
    @NotNull(message = "Status is required")
    @Column(nullable = false)
    private Integer status;
    
    // 6. Reason for Appointment (New field)
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    @Column(name = "reason", length = 500)
    private String reason;

    // Default constructor required by JPA
    public Appointment() {
    }

    // Parameterized constructor (Updated to include reason)
    public Appointment(Doctor doctor, Patient patient, LocalDateTime appointmentTime, Integer status, String reason) {
        this.doctor = doctor;
        this.patient = patient;
        this.appointmentTime = appointmentTime;
        this.status = status;
        this.reason = reason;
    }

    // 7. Transient method: Calculates the estimated end time (AppointmentTime + 1 hour).
    @Transient
    public LocalDateTime getEndTime() {
        if (appointmentTime == null) return null;
        return appointmentTime.plusHours(1);
    }

    // 8. Transient method: Extracts the date part of the appointment time.
    @Transient
    public LocalDate getAppointmentDate() {
        if (appointmentTime == null) return null;
        return appointmentTime.toLocalDate();
    }

    // 9. Transient method: Extracts the time part of the appointment time.
    @Transient
    public LocalTime getAppointmentTimeOnly() {
        if (appointmentTime == null) return null;
        return appointmentTime.toLocalTime();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
    
    // New Getter for Reason
    public String getReason() {
        return reason;
    }

    // New Setter for Reason
    public void setReason(String reason) {
        this.reason = reason;
    }
}
