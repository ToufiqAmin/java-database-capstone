package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.DTO.DoctorDTO;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;
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
 * REST controller for managing Doctor entities and related operations, including CRUD,
 * availability checking, and login. Requires appropriate tokens for authorization.
 * * FIXES:
 * 1. Corrected date passing in getDoctorAvailability: Passing LocalDate directly, 
 * removing the incompatible java.sql.Date conversion.
 * 2. Corrected login arguments: Passing email and password separately to doctorService.validateDoctor.
 * 3. Corrected updateDoctor arguments: Passing only the Doctor object to the service method.
 */
@RestController // 1. Designates this class as a REST controller
@RequestMapping("${api.path}doctor") // Sets the base path for all endpoints
public class DoctorController {

    private final DoctorService doctorService;
    private final CentralService service;

    // 2. Constructor injection for dependencies
    @Autowired
    public DoctorController(DoctorService doctorService, CentralService service) {
        this.doctorService = doctorService;
        this.service = service;
    }

    /**
     * 1. Get Doctor Availability: Fetches available slots for a doctor on a given date.
     * Endpoint: GET /api/doctor/availability/{user}/{doctorId}/{date}/{token}
     * Requires: Valid token (role specified by {user})
     */
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<?> getDoctorAvailability(
            @PathVariable String user,
            @PathVariable Long doctorId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String token
    ) {
        // Validate token for the specified user role
        ResponseEntity<Map<String, String>> validationResponse = service.validateToken(token, user);
        if (validationResponse != null) {
            return validationResponse;
        }

        try {
            // FIX: Passing LocalDate directly. Assumes doctorService.getDoctorAvailability expects LocalDate.
            List<?> availableSlots = doctorService.getDoctorAvailability(doctorId, date);
            return ResponseEntity.ok(Map.of("availableSlots", availableSlots));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to fetch availability."));
        }
    }

    /**
     * 2. Get List of Doctors: Fetches a list of all registered doctors.
     * Endpoint: GET /api/doctor
     */
    @GetMapping
    public ResponseEntity<Map<String, List<DoctorDTO>>> getDoctor() {
        // Fetch all doctors and convert them to DTOs (Data Transfer Objects) to exclude sensitive info.
        List<DoctorDTO> doctorDTOs = doctorService.getDoctors()
                .stream()
                // DoctorDTO must be correctly implemented to map Doctor fields without the password
                .map(DoctorDTO::new) 
                .toList();
        return ResponseEntity.ok(Map.of("doctors", doctorDTOs));
    }

    /**
     * 3. Add New Doctor: Registers a new doctor (Admin only).
     * Endpoint: POST /api/doctor/{token}
     * Requires: Admin token
     */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> saveDoctor(@RequestBody Doctor doctor, @PathVariable String token) {
        // Validate token for "admin" role
        ResponseEntity<Map<String, String>> validationResponse = service.validateToken(token, "admin");
        if (validationResponse != null) {
            return validationResponse;
        }

        int result = doctorService.saveDoctor(doctor);
        Map<String, String> response = new HashMap<>();

        return switch (result) {
            case -1 -> {
                response.put("message", "Doctor already exists");
                yield ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }
            case 1 -> {
                response.put("message", "Doctor added to db");
                yield ResponseEntity.status(HttpStatus.CREATED).body(response);
            }
            default -> {
                response.put("message", "Some internal error occurred");
                yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        };
    }

    /**
     * 4. Doctor Login: Authenticates a doctor and issues a token.
     * Endpoint: POST /api/doctor/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> doctorLogin(@RequestBody Login login) {
        // FIX: Delegate validation by passing required separate email and password strings.
        return doctorService.validateDoctor(login.getIdentifier(), login.getPassword());
    }

    /**
     * 5. Update Doctor Details: Updates an existing doctor's information (Admin only).
     * Endpoint: PUT /api/doctor/{token} (Doctor ID expected in the request body)
     * Requires: Admin token
     */
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateDoctor(@RequestBody Doctor updatedDoctor, @PathVariable String token) {
        // Validate token for "admin" role
        ResponseEntity<Map<String, String>> validationResponse = service.validateToken(token, "admin");
        if (validationResponse != null) {
            return validationResponse;
        }

        // FIX: The service method is expected to take only the Doctor object based on the error.
        int result = doctorService.updateDoctor(updatedDoctor);
        Map<String, String> response = new HashMap<>();

        return switch (result) {
            case -1 -> {
                response.put("message", "Doctor not found");
                yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            case 1 -> {
                response.put("message", "Doctor updated");
                yield ResponseEntity.ok(response);
            }
            default -> {
                response.put("message", "Some internal error occurred");
                yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        };
    }

    /**
     * 6. Delete Doctor: Removes a doctor by ID (Admin only).
     * Endpoint: DELETE /api/doctor/{id}/{token}
     * Requires: Admin token
     */
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> deleteDoctor(@PathVariable Long id, @PathVariable String token) {
        // Validate token for "admin" role
        ResponseEntity<Map<String, String>> validationResponse = service.validateToken(token, "admin");
        if (validationResponse != null) {
            return validationResponse;
        }

        int result = doctorService.deleteDoctor(id);
        Map<String, String> response = new HashMap<>();

        return switch (result) {
            case -1 -> {
                response.put("message", "Doctor not found with id " + id);
                yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            case 1 -> {
                response.put("message", "Doctor deleted successfully");
                yield ResponseEntity.ok(response);
            }
            default -> {
                response.put("message", "Some internal error occurred");
                yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        };
    }

    /**
     * 7. Filter Doctors: Filters doctors based on optional criteria (name, time, specialty).
     * Endpoint: GET /api/doctor/filter?name={name}&time={time}&speciality={speciality}
     * FIX: Delegate filtering to DoctorService directly for core domain logic.
     */
    @GetMapping("/filter")
    public ResponseEntity<Map<String, List<DoctorDTO>>> filterDoctor(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String speciality
    ) {
        // Delegate filtering logic to the specific DoctorService (assuming filterDoctors returns List<Doctor>)
        // Assuming the service method signature is: filterDoctors(String name, String speciality, String time)
        List<Doctor> doctors = doctorService.filterDoctors(name, speciality, time);
        
        // Convert the list of Doctor entities to DoctorDTOs
        List<DoctorDTO> doctorDTOs = doctors.stream()
                .map(DoctorDTO::new)
                .toList();

        // Return the list wrapped in a "doctors" map key for consistency
        return ResponseEntity.ok(Map.of("doctors", doctorDTOs));
    }
}
