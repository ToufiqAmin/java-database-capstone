package com.project.back_end.controllers;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.CentralService; // Corrected to use the standard Service class
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing Patient entities and related operations, including
 * registration, login, and appointment filtering/retrieval.
 */
@RestController // 1. Mark as REST controller
@RequestMapping("/patient") // Group all patient operations under /patient
public class PatientController {

    private final PatientService patientService;
    // NOTE: 'Service service' uses a generic name. Assuming this refers to a common utility service.
    private final CentralService service; // Corrected dependency type from CentralService to Service

    // 2. Constructor-based injection for dependencies
    @Autowired
    public PatientController(PatientService patientService, CentralService service) { // Corrected constructor parameter type
        this.patientService = patientService;
        this.service = service;
    }

    /**
     * 3. Get patient details using token.
     * Endpoint: GET /patient/me/{token}
     * Requires: Valid patient token
     */
    @GetMapping("/me/{token}")
    public ResponseEntity<?> getPatient(@PathVariable String token) {
        // Validate token for "patient" role
        ResponseEntity<Map<String, String>> validationResponse = service.validateToken(token, "patient");
        if (validationResponse != null) {
            return validationResponse;
        }

        // Fetch patient details using the token
        Patient patient = patientService.getPatientDetails(token);
        if (patient == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Patient not found."));
        }

        return ResponseEntity.ok(patient);
    }

    /**
     * 4. Register a new patient.
     * Endpoint: POST /patient/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> createPatient(@RequestBody Patient patient) {
        // Validate if patient already exists (by email/phone)
        if (!service.validatePatient(patient)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Patient with email id or phone no already exist"));
        }

        int result = patientService.createPatient(patient);
        return switch (result) {
            case 1 -> ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Signup successful"));
            // Assuming 0 or other negative means internal error
            case 0 -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Internal server error"));
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Unexpected error during signup."));
        };
    }

    /**
     * 5. Login patient.
     * Endpoint: POST /patient/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Login login) {
        // Delegate login validation and token issuance to the service
        // The service layer method is expected to return ResponseEntity<Map<String, String>>
        return service.validatePatientLogin(login);
    }

    /**
     * 6. Get appointments for a specific patient.
     * Endpoint: GET /patient/appointments/{patientId}/{token}
     * Requires: Patient token
     */
    @GetMapping("/appointments/{patientId}/{token}")
    public ResponseEntity<?> getPatientAppointments(@PathVariable Long patientId,
                                                    @PathVariable String token) {
        // Validate token for "patient" role
        ResponseEntity<Map<String, String>> validationResponse = service.validateToken(token, "patient");
        if (validationResponse != null) {
            return validationResponse;
        }

        // Fetch appointments - FIXED: Added 'token' as an argument
        List<AppointmentDTO> appointments = patientService.getPatientAppointment(patientId, token);
        return ResponseEntity.ok(appointments);
    }

    /**
     * 7. Filter appointments (past/future/doctor name).
     * Endpoint: GET /patient/appointments/filter?condition={condition}&name={name}&token={token}
     * Requires: Patient token
     */
    @GetMapping("/appointments/filter")
    public ResponseEntity<?> filterPatientAppointment(@RequestParam(required = false) String condition,
                                                     @RequestParam(required = false) String name,
                                                     @RequestParam String token) {
        // Validate token for "patient" role
        ResponseEntity<Map<String, String>> validationResponse = service.validateToken(token, "patient");
        if (validationResponse != null) {
            return validationResponse;
        }

        // Get Patient ID from token
        Patient patient = patientService.getPatientDetails(token);
        if (patient == null || patient.getId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid token or patient data not found."));
        }
        Long patientId = patient.getId();
        List<AppointmentDTO> filtered;

        // Determine which specific PatientService filtering method to call
        if (condition != null && name != null) {
            filtered = patientService.filterByDoctorAndCondition(condition, name, patientId);
        } else if (condition != null) {
            filtered = patientService.filterByCondition(condition, patientId);
        } else if (name != null) {
            filtered = patientService.filterByDoctor(name, patientId);
        } else {
            // If no filter parameters are provided, return all appointments
            filtered = patientService.getPatientAppointment(patientId, token);
        }

        return ResponseEntity.ok(filtered);
    }
}
