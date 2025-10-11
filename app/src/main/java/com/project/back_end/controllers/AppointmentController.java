package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.CentralService; // Using the central Service class
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all REST operations related to appointments, including booking, retrieval,
 * updating, and cancellation, with built-in token and slot validation.
 */
@RestController // 1. Mark as REST controller
@RequestMapping("/appointments") // Base path for appointment operations
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final CentralService service;

    // 2. Constructor injection for dependencies
    @Autowired
    public AppointmentController(AppointmentService appointmentService, CentralService service) {
        this.appointmentService = appointmentService;
        this.service = service;
    }

    /**
     * 3. Retrieves appointments for a doctor on a specific date, optionally filtered by patient name.
     * Endpoint: GET /appointments/{token}/{date}?patientName={name}
     * Requires: Doctor token
     */
    @GetMapping("/{token}/{date}")
    public ResponseEntity<?> getAppointments(
            @PathVariable String token,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String patientName
    ) {
        // Validate token for "doctor" role
        ResponseEntity<Map<String, String>> validationResponse = service.validateToken(token, "doctor");
        if (validationResponse != null) {
            return validationResponse;
        }

        // Extract the Doctor ID from the valid token
        Long doctorId = service.extractUserIdFromToken(token);
        if (doctorId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Could not identify Doctor from token."));
        }

        try {
            // FIX: Using the correct service method for filtering appointments. 
            // Assuming filterAppointmentsForDoctor signature is (Long doctorId, String patientName, LocalDate date).
            List<?> appointments = appointmentService.filterAppointmentsForDoctor(doctorId, patientName, date);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to retrieve appointments."));
        }
    }

    /**
     * 4. Books a new appointment after token and slot validation.
     * Endpoint: POST /appointments/book/{token}
     * Requires: Patient token
     */
    @PostMapping("/book/{token}")
    public ResponseEntity<Map<String, String>> bookAppointment(@PathVariable String token, @RequestBody Appointment appointment) {
        // Validate token for "patient" role
        ResponseEntity<Map<String, String>> validationResponse = service.validateToken(token, "patient");
        if (validationResponse != null) {
            return validationResponse;
        }

        // Validate appointment slot availability
        int validationCode = service.validateAppointment(appointment);

        if (validationCode == -1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Doctor not found."));
        } else if (validationCode == 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Appointment slot is not available or appointment time is invalid."));
        }

        // Book the appointment (validationCode == 1)
        // FIX: Handle the int return code from appointmentService.bookAppointment and map it to ResponseEntity.
        int result = appointmentService.bookAppointment(appointment, token);
        Map<String, String> response = new HashMap<>();

        return switch (result) {
            case 1 -> {
                response.put("message", "Appointment booked successfully.");
                yield ResponseEntity.status(HttpStatus.CREATED).body(response);
            }
            case -1 -> {
                response.put("message", "Slot conflict detected.");
                yield ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            case -2 -> {
                response.put("message", "Doctor not found.");
                yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            case -3 -> {
                response.put("message", "Patient not found (invalid token association).");
                yield ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            default -> {
                response.put("message", "Internal server error during booking.");
                yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        };
    }

    /**
     * 5. Updates an existing appointment.
     * Endpoint: PUT /appointments/update/{token}
     * Requires: Patient token
     */
    @PutMapping("/update/{token}")
    public ResponseEntity<Map<String, String>> updateAppointment(@PathVariable String token,
                                                     @RequestBody Appointment updatedAppointment) {
        // Validate token for "patient" role
        ResponseEntity<Map<String, String>> validationResponse = service.validateToken(token, "patient");
        if (validationResponse != null) {
            return validationResponse;
        }

        // FIX: Handle the int return code from appointmentService.updateAppointment and map it to ResponseEntity.
        int result = appointmentService.updateAppointment(updatedAppointment, token);
        Map<String, String> response = new HashMap<>();

        return switch (result) {
            case 1 -> {
                response.put("message", "Appointment updated successfully.");
                yield ResponseEntity.ok(response);
            }
            case -1 -> {
                response.put("message", "Appointment not found.");
                yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            case -2 -> {
                response.put("message", "Unauthorized to update this appointment.");
                yield ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            default -> {
                response.put("message", "Internal server error during update.");
                yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        };
    }

    /**
     * 6. Cancels an appointment.
     * Endpoint: DELETE /appointments/cancel/{id}/{token}
     * Requires: Patient token
     */
    @DeleteMapping("/cancel/{id}/{token}")
    public ResponseEntity<Map<String, String>> cancelAppointment(@PathVariable Long id, @PathVariable String token) {
        // Validate token for "patient" role
        ResponseEntity<Map<String, String>> validationResponse = service.validateToken(token, "patient");
        if (validationResponse != null) {
            return validationResponse;
        }

        // Extract the Patient ID from the valid token to ensure ownership check in the service layer
        Long patientId = service.extractUserIdFromToken(token);
        if (patientId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Could not identify Patient from token."));
        }

        // FIX: Pass the appointment ID and the token (String), not the patient ID (Long),
        // to match the service method signature public int cancelAppointment(long id, String token)
        int result = appointmentService.cancelAppointment(id, token);
        Map<String, String> response = new HashMap<>();

        return switch (result) {
            case 1 -> {
                response.put("message", "Appointment cancelled successfully.");
                yield ResponseEntity.ok(response);
            }
            case -1 -> {
                response.put("message", "Appointment not found.");
                yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            case -2 -> {
                response.put("message", "Unauthorized to cancel this appointment.");
                yield ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            default -> {
                response.put("message", "Internal server error during cancellation.");
                yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        };
    }
}
