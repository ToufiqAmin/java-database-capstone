package com.project.back_end.controllers;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.PrescriptionService;
import com.project.back_end.services.CentralService; // Autowired Service for common functionality (token validation)
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map; // Required for ResponseEntity<Map<String, String>> responses

/**
 * REST controller for managing Prescription entities.
 * Handles doctor-only operations for saving and retrieving prescriptions.
 */
@RestController // 1. Designates this class as a REST controller
@RequestMapping("${api.path}prescription") // Sets the base URL path (e.g., /api/prescription)
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final AppointmentService appointmentService;
    private final CentralService service; // Common service for validation

    // 2. Constructor injection for dependencies
    @Autowired
    public PrescriptionController(PrescriptionService prescriptionService,
                                   AppointmentService appointmentService,
                                   CentralService service) {
        this.prescriptionService = prescriptionService;
        this.appointmentService = appointmentService;
        this.service = service;
    }

    /**
     * 3. Save a new prescription (Doctor access only).
     * Endpoint: POST /.../prescription/{token}
     * @param prescription The prescription details from the request body.
     * @param token The doctor's authentication token.
     * @return Success message or unauthorized status.
     */
    @PostMapping("/{token}")
    public ResponseEntity<?> savePrescription(@RequestBody Prescription prescription, @PathVariable String token) {
        // Token validation: must be a doctor
        ResponseEntity<Map<String, String>> validationResponse = service.validateToken(token, "doctor");
        if (validationResponse != null) {
            return validationResponse; // Returns UNAUTHORIZED or other error map
        }

        // Extract the doctor's identity (email or ID) from the token for authorization/logging in the service.
        String doctorIdentity = service.getTokenId(token);

        // Update appointment status to 'Completed' (status code 1) after prescription is issued
        // FIX: Added doctorIdentity as the required third argument for authorization/tracking
        int appointmentStatusUpdate = appointmentService.changeAppointmentStatus(
            prescription.getAppointmentId(), 
            1,
            doctorIdentity // Pass the doctor's identity (from token)
        );
        Map<String, String> errorResponse = new HashMap<>();

        // Check if the appointment status update failed (assuming -1 means Not Found)
        if (appointmentStatusUpdate == -1) {
            errorResponse.put("message", "Appointment not found for ID: " + prescription.getAppointmentId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } else if (appointmentStatusUpdate != 1) {
            // Handle other failure cases (e.g., database error)
            errorResponse.put("message", "Failed to update appointment status before saving prescription.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }

        // Delegate saving logic to the PrescriptionService (Only proceeds if appointment status was updated successfully)
        return prescriptionService.savePrescription(prescription);
    }

    /**
     * 4. Get a prescription by appointment ID (Doctor access only).
     * Endpoint: GET /.../prescription/{appointmentId}/{token}
     * @param appointmentId The ID of the appointment.
     * @param token The doctor's authentication token.
     * @return Prescription details or not found status.
     */
    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<?> getPrescription(@PathVariable Long appointmentId, @PathVariable String token) {
        // Token validation: must be a doctor
        ResponseEntity<Map<String, String>> validationResponse = service.validateToken(token, "doctor");
        if (validationResponse != null) {
            return validationResponse;
        }

        // Delegate retrieval logic to the PrescriptionService
        return prescriptionService.getPrescription(appointmentId);
    }
}
