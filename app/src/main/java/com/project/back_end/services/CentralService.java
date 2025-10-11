package com.project.back_end.services;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.DTO.Login; // Login is likely a DTO for credentials
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A central service class coordinating authentication, validation, doctor/patient
 * management, and appointment logic across various repositories and services.
 * * FIXES: 
 * 1. Corrected method names used on DoctorService to resolve 'cannot find symbol' errors.
 * 2. Corrected date conversion in validateAppointment (java.sql.Date -> java.time.LocalDate).
 * 3. Standardized token email extraction to extractIdentifierFromToken (FIX: Reverted to extractEmailFromToken to resolve actual method name conflict).
 * 4. ADDED: extractUserIdFromToken to support ID retrieval based on the token identifier.
 * 5. FIX: Renamed class from 'Service' to 'CentralService' to avoid compilation conflict.
 * 6. FIX: Refactored filterPatient to correctly wrap List<AppointmentDTO> from PatientService into ResponseEntity.
 */
@SuppressWarnings("unchecked") // Suppress warning for casting in filterPatient
@Service 
public class CentralService { // Renamed class

    // Dependencies (Repositories and other Services)
    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    // Constructor Injection
    public CentralService(TokenService tokenService, // Renamed constructor
                   AdminRepository adminRepository,
                   DoctorRepository doctorRepository,
                   PatientRepository patientRepository,
                   DoctorService doctorService,
                   PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    /**
     * Extracts the user's Long ID (Doctor or Patient) from a valid token.
     * The token's identifier (email/username) is used to look up the corresponding entity.
     * This method is required by controllers (like AppointmentController) to enforce ownership.
     * @param token The authenticated token.
     * @return The Long ID of the user (Doctor or Patient), or null if the user is not found.
     */
    public Long extractUserIdFromToken(String token) {
        try {
            // 1. Get the unique identifier (email/username) from the token payload
            // FIX: Reverting to extractEmailFromToken based on compilation errors in other files
            String identifier = tokenService.extractEmailFromToken(token); 
            if (identifier == null) return null;

            // 2. Try to find the user as a Patient
            Patient patient = patientRepository.findByEmail(identifier);
            if (patient != null) {
                return patient.getId();
            }

            // 3. Try to find the user as a Doctor (assuming DoctorRepository has findByEmail or similar)
            // Note: In a real system, the token should contain the role to avoid searching multiple repositories.
            Doctor doctor = doctorRepository.findByEmail(identifier); 
            if (doctor != null) {
                return doctor.getId();
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks the validity of a token for a given user role.
     * @param token The token to be validated.
     * @param user The user/role to whom the token belongs.
     * @return ResponseEntity with an error message (UNAUTHORIZED) if the token is invalid or expired, or null on success.
     */
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        try {
            if (tokenService.validateToken(token, user)) {
                return null; // Token is valid, return null to indicate no error response needed
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid or expired token.");
                return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Token validation failed due to an internal error.");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validates the login credentials of an admin.
     * @param receivedAdmin The admin credentials (username and password) to be validated.
     * @return ResponseEntity with a generated token on success (OK) or an error message (UNAUTHORIZED/INTERNAL_SERVER_ERROR).
     */
    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> response = new HashMap<>();
        try {
            Admin admin = adminRepository.findByUsername(receivedAdmin.getUsername());
            
            // Check existence and password match
            if (admin == null || !admin.getPassword().equals(receivedAdmin.getPassword())) {
                response.put("message", "Invalid username or password.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            
            // Generate token (assuming tokenService.generateToken accepts identifier string)
            String token = tokenService.generateToken(admin.getUsername()); 
            response.put("token", token);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Login failed due to an internal error.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Filters doctors based on name, specialty, and/or available time.
     * * FIX: Corrected method names called on doctorService to resolve 'cannot find symbol' errors.
     * * @param name The name of the doctor.
     * @param specialty The specialty of the doctor.
     * @param time The available time of the doctor.
     * @return Map<String, Object> containing a list of doctors that match the filtering criteria.
     */
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        Map<String, Object> response = new HashMap<>();
        List<Doctor> doctors;

        // Delegate filtering logic to DoctorService based on provided parameters
        if (name != null && specialty != null && time != null) {
            doctors = doctorService.filterDoctorsByNameSpecialtyAndTime(name, specialty, time);
        } else if (name != null && specialty != null) {
            // FIX: Corrected method name to standard plural convention
            doctors = doctorService.filterDoctorsByNameAndSpecialty(name, specialty); 
        } else if (name != null && time != null) {
            // Assuming this method exists on DoctorService
            doctors = doctorService.filterDoctorsByNameAndTime(name, time);
        } else if (specialty != null && time != null) {
            // FIX: Corrected method name to standard plural convention
            doctors = doctorService.filterDoctorsByTimeAndSpecialty(time, specialty); 
        } else if (name != null) {
            // Assuming this method exists on DoctorService
            doctors = doctorService.filterDoctorsByName(name);
        } else if (specialty != null) {
            // FIX: Corrected method name to standard plural convention
            doctors = doctorService.filterDoctorsBySpecialty(specialty); 
        } else if (time != null) {
            // Assuming this method exists on DoctorService
            doctors = doctorService.filterDoctorsByTime(time);
        } else {
            doctors = doctorService.getDoctors(); // Return all if no filters provided
        }

        response.put("doctors", doctors);
        response.put("count", doctors.size());
        return response;
    }

    /**
     * Validates whether an appointment is available based on the doctor's schedule.
     * * FIX: Corrected the date conversion from java.sql.Date to java.time.LocalDate.
     * * @param appointment The appointment to validate.
     * @return 1 if the appointment time is valid, 0 if the time is unavailable, -1 if the doctor doesn't exist.
     */
    public int validateAppointment(Appointment appointment) {
        try {
            Long doctorId = appointment.getDoctor().getId();
            LocalDate date = appointment.getAppointmentTime().toLocalDate();
            LocalTime time = appointment.getAppointmentTime().toLocalTime();

            // Check if doctor exists
            Optional<Doctor> optional = doctorRepository.findById(doctorId);
            if (optional.isEmpty()) return -1;

            // Check doctor's availability slots
            // FIX: Passing java.time.LocalDate 'date' directly, avoiding the java.sql.Date conversion error (Line 157 in original log)
            List<String> availableSlots = doctorService.getDoctorAvailability(doctorId, date);
            
            // Convert requested time to string format for comparison (e.g., "HH:MM:SS")
            String requestedTimeStr = time.toString(); 
            
            return availableSlots.contains(requestedTimeStr) ? 1 : 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            // Treat internal error as unavailable for safety
            return 0; 
        }
    }

    /**
     * Checks whether a patient with the same email or phone number already exists.
     * @param patient The patient to validate.
     * @return true if the patient does not exist (valid for registration), false if the patient exists already.
     */
    public boolean validatePatient(Patient patient) {
        // patientRepository.findByEmailOrPhone returns a Patient if found, or null otherwise.
        return patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone()) == null;
    }

    /**
     * Validates a patient's login credentials (email and password).
     * @param login The login credentials of the patient (email and password).
     * @return ResponseEntity with a generated token on success (OK) or an error message (UNAUTHORIZED/INTERNAL_SERVER_ERROR).
     */
    public ResponseEntity<Map<String, String>> validatePatientLogin(Login login) {
        Map<String, String> response = new HashMap<>();
        try {
            // Using a generic identifier getter to fix potential DTO issues
            String identifier = login.getIdentifier(); 
            Patient patient = patientRepository.findByEmail(identifier);
            
            // Check existence and password match
            if (patient == null || !patient.getPassword().equals(login.getPassword())) {
                response.put("message", "Invalid email or password.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }
            
            // Generate token using patient's email
            String token = tokenService.generateToken(patient.getEmail()); 
            response.put("token", token);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", "Login failed due to an internal error.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Filters patient appointments based on condition and/or doctor name.
     * * FIX: Changed tokenService.extractEmailFromToken to tokenService.extractIdentifierFromToken
     * to resolve 'cannot find symbol' error.
     * * REFACTOR: Uses extractUserIdFromToken to centralize ID extraction logic.
     * * FIX: Corrected return type incompatibility by wrapping the List result from PatientService in ResponseEntity.
     * * @param condition The medical condition to filter appointments by (past/future).
     * @param name The doctor's name to filter appointments by.
     * @param token The authentication token to identify the patient.
     * @return ResponseEntity with the filtered list of patient appointments or an error message.
     */
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        Map<String, Object> responseMap = new HashMap<>();
        
        try {
            // 1. Get the patient ID from the token using the centralized helper method
            Long patientId = extractUserIdFromToken(token); 

            if (patientId == null) {
                responseMap.put("message", "Patient not authorized or not found.");
                return new ResponseEntity<>(responseMap, HttpStatus.UNAUTHORIZED);
            }
            
            List<AppointmentDTO> filteredAppointments; // Variable to hold the list of DTOs
            
            // 2. Delegate filtering to PatientService
            // Assuming these methods in PatientService return List<AppointmentDTO>
            if (condition != null && name != null) {
                filteredAppointments = patientService.filterByDoctorAndCondition(condition, name, patientId);
            } else if (name != null) {
                filteredAppointments = patientService.filterByDoctor(name, patientId);
            } else if (condition != null) {
                filteredAppointments = patientService.filterByCondition(condition, patientId);
            } else {
                // No filters provided, get all appointments 
                filteredAppointments = patientService.getPatientAppointment(patientId, token);
            }
            
            // 3. Construct the final response entity
            responseMap.put("appointments", filteredAppointments);
            responseMap.put("count", filteredAppointments.size());
            return new ResponseEntity<>(responseMap, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            responseMap.put("error", "Error filtering appointments.");
            return new ResponseEntity<>(responseMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
