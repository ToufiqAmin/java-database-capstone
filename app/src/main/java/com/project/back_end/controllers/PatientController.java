package com.project.back_end.controllers;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.CentralService;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // 1. Mark as REST controller
@RequestMapping("/patient") // Group all patient operations under /patient
public class PatientController {

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST API controller for patient-related operations.
//    - Use `@RequestMapping("/patient")` to prefix all endpoints with `/patient`, grouping all patient functionalities under a common route.


// 2. Autowire Dependencies:
//    - Inject `PatientService` to handle patient-specific logic such as creation, retrieval, and appointments.
//    - Inject the shared `Service` class for tasks like token validation and login authentication.


// 3. Define the `getPatient` Method:
//    - Handles HTTP GET requests to retrieve patient details using a token.
//    - Validates the token for the `"patient"` role using the shared service.
//    - If the token is valid, returns patient information; otherwise, returns an appropriate error message.


// 4. Define the `createPatient` Method:
//    - Handles HTTP POST requests for patient registration.
//    - Accepts a validated `Patient` object in the request body.
//    - First checks if the patient already exists using the shared service.
//    - If validation passes, attempts to create the patient and returns success or error messages based on the outcome.


// 5. Define the `login` Method:
//    - Handles HTTP POST requests for patient login.
//    - Accepts a `Login` DTO containing email/username and password.
//    - Delegates authentication to the `validatePatientLogin` method in the shared service.
//    - Returns a response with a token or an error message depending on login success.


// 6. Define the `getPatientAppointment` Method:
//    - Handles HTTP GET requests to fetch appointment details for a specific patient.
//    - Requires the patient ID, token, and user role as path variables.
//    - Validates the token using the shared service.
//    - If valid, retrieves the patient's appointment data from `PatientService`; otherwise, returns a validation error.


// 7. Define the `filterPatientAppointment` Method:
//    - Handles HTTP GET requests to filter a patient's appointments based on specific conditions.
//    - Accepts filtering parameters: `condition`, `name`, and a token.
//    - Token must be valid for a `"patient"` role.
//    - If valid, delegates filtering logic to the shared service and returns the filtered result.

    private final PatientService patientService;
    private final CentralService service;

    // 2. Constructor-based injection
    //@Autowired
    public PatientController(PatientService patientService, CentralService service) {
        this.patientService = patientService;
        this.service = service;
    }

    // 3. Get patient details using token
    @GetMapping("/me/{token}")
    public ResponseEntity<?> getPatient(@PathVariable String token) {
        if (!service.validateToken(token, "patient")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }

        Patient patient = patientService.getPatientDetails(token);
        if (patient == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Patient not found.");
        }

        return ResponseEntity.ok(patient);
    }

    // 4. Register a new patient
    @PostMapping("/register")
    public ResponseEntity<?> createPatient(@RequestBody Patient patient) {
        if (!service.validatePatient(patient)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Patient already exists with given email or phone.");
        }

        int result = patientService.createPatient(patient);
        return switch (result) {
            case 1 -> ResponseEntity.status(HttpStatus.CREATED).body("Patient registered successfully.");
            case 0 -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving patient.");
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unexpected error.");
        };
    }

    // 5. Login patient
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login login) {
        return service.validatePatientLogin(login.getEmail(), login.getPassword());
    }

    // 6. Get appointments for patient
    @GetMapping("/appointments/{patientId}/{user}/{token}")
    public ResponseEntity<?> getPatientAppointments(@PathVariable Long patientId,
                                                    @PathVariable String user,
                                                    @PathVariable String token) {
        if (!service.validateToken(token, user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }

        List<AppointmentDTO> appointments = patientService.getPatientAppointment(patientId);
        return ResponseEntity.ok(appointments);
    }

    // 7. Filter appointments (past/future/doctor name)
    @GetMapping("/appointments/filter")
    public ResponseEntity<?> filterPatientAppointment(@RequestParam(required = false) String condition,
                                                      @RequestParam(required = false) String name,
                                                      @RequestParam String token) {
        if (!service.validateToken(token, "patient")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }

        List<AppointmentDTO> filtered = service.filterPatient(token, condition, name);
        return ResponseEntity.ok(filtered);
    }

}


