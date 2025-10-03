package com.project.back_end.controllers;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.CentralService;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.time.LocalDate;
//import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController // 1. REST API Controller
@RequestMapping("${api.path}doctor") // e.g., /api/doctor
public class DoctorController {

// 1. Set Up the Controller Class:
//    - Annotate the class with `@RestController` to define it as a REST controller that serves JSON responses.
//    - Use `@RequestMapping("${api.path}doctor")` to prefix all endpoints with a configurable API path followed by "doctor".
//    - This class manages doctor-related functionalities such as registration, login, updates, and availability.


// 2. Autowire Dependencies:
//    - Inject `DoctorService` for handling the core logic related to doctors (e.g., CRUD operations, authentication).
//    - Inject the shared `Service` class for general-purpose features like token validation and filtering.


// 3. Define the `getDoctorAvailability` Method:
//    - Handles HTTP GET requests to check a specific doctor’s availability on a given date.
//    - Requires `user` type, `doctorId`, `date`, and `token` as path variables.
//    - First validates the token against the user type.
//    - If the token is invalid, returns an error response; otherwise, returns the availability status for the doctor.


// 4. Define the `getDoctor` Method:
//    - Handles HTTP GET requests to retrieve a list of all doctors.
//    - Returns the list within a response map under the key `"doctors"` with HTTP 200 OK status.


// 5. Define the `saveDoctor` Method:
//    - Handles HTTP POST requests to register a new doctor.
//    - Accepts a validated `Doctor` object in the request body and a token for authorization.
//    - Validates the token for the `"admin"` role before proceeding.
//    - If the doctor already exists, returns a conflict response; otherwise, adds the doctor and returns a success message.


// 6. Define the `doctorLogin` Method:
//    - Handles HTTP POST requests for doctor login.
//    - Accepts a validated `Login` DTO containing credentials.
//    - Delegates authentication to the `DoctorService` and returns login status and token information.


// 7. Define the `updateDoctor` Method:
//    - Handles HTTP PUT requests to update an existing doctor's information.
//    - Accepts a validated `Doctor` object and a token for authorization.
//    - Token must belong to an `"admin"`.
//    - If the doctor exists, updates the record and returns success; otherwise, returns not found or error messages.


// 8. Define the `deleteDoctor` Method:
//    - Handles HTTP DELETE requests to remove a doctor by ID.
//    - Requires both doctor ID and an admin token as path variables.
//    - If the doctor exists, deletes the record and returns a success message; otherwise, responds with a not found or error message.


// 9. Define the `filter` Method:
//    - Handles HTTP GET requests to filter doctors based on name, time, and specialty.
//    - Accepts `name`, `time`, and `speciality` as path variables.
//    - Calls the shared `Service` to perform filtering logic and returns matching doctors in the response.

    private final DoctorService doctorService;
    private final CentralService service;

    // 2. Constructor injection
    //@Autowired
    public DoctorController(DoctorService doctorService, CentralService service) {
        this.doctorService = doctorService;
        this.service = service;
    }

    // 3. Get doctor availability
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<?> getDoctorAvailability(
            @PathVariable String user,
            @PathVariable Long doctorId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String token
    ) {
        if (!service.validateToken(token, user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }

        List<?> availableSlots = doctorService.getDoctorAvailability(doctorId, Date.valueOf(date));
        return ResponseEntity.ok(Map.of("availableSlots", availableSlots));
    }

    // 4. Get all doctors
    @GetMapping("/all")
    public ResponseEntity<?> getDoctor() {
        List<Doctor> doctors = doctorService.getDoctors();
        return ResponseEntity.ok(Map.of("doctors", doctors));
    }

    // 5. Save a new doctor (admin-only)
    @PostMapping("/register/{token}")
    public ResponseEntity<?> saveDoctor(@RequestBody Doctor doctor, @PathVariable String token) {
        if (!service.validateToken(token, "admin")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access.");
        }

        int result = doctorService.saveDoctor(doctor);
        return switch (result) {
            case -1 -> ResponseEntity.status(HttpStatus.CONFLICT).body("Doctor with email already exists.");
            case 1 -> ResponseEntity.status(HttpStatus.CREATED).body("Doctor registered successfully.");
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error while registering doctor.");
        };
    }

    // 6. Doctor login
    @PostMapping("/login")
    public ResponseEntity<?> doctorLogin(@RequestBody Login login) {
        return ResponseEntity.ok(doctorService.validateDoctor(login.getEmail(), login.getPassword()));
    }

    // 7. Update doctor (admin-only)
    @PutMapping("/update/{token}/{doctorId}")
    public ResponseEntity<?> updateDoctor(@RequestBody Doctor updatedDoctor,
                                          @PathVariable String token,
                                          @PathVariable Long doctorId) {
        if (!service.validateToken(token, "admin")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access.");
        }

        int result = doctorService.updateDoctor(doctorId, updatedDoctor);
        return switch (result) {
            case -1 -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Doctor not found.");
            case 1 -> ResponseEntity.ok("Doctor updated successfully.");
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error while updating doctor.");
        };
    }

    // 8. Delete doctor (admin-only)
    @DeleteMapping("/delete/{token}/{doctorId}")
    public ResponseEntity<?> deleteDoctor(@PathVariable String token, @PathVariable Long doctorId) {
        if (!service.validateToken(token, "admin")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access.");
        }

        int result = doctorService.deleteDoctor(doctorId);
        return switch (result) {
            case -1 -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Doctor not found.");
            case 1 -> ResponseEntity.ok("Doctor and associated appointments deleted successfully.");
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error while deleting doctor.");
        };
    }

    // 9. Filter doctors (by name, time, specialty)
    @GetMapping("/filter")
    public ResponseEntity<?> filterDoctor(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String speciality
    ) {
        List<Doctor> doctors = service.filterDoctor(name, speciality, time);
        return ResponseEntity.ok(Map.of("doctors", doctors));
    }
}
