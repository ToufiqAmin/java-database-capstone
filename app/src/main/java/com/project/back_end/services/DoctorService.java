package com.project.back_end.services;

import com.project.back_end.models.Doctor;
import com.project.back_end.models.Appointment;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class to manage operations related to doctors, including retrieving availability, 
 * saving, updating, deleting, and filtering doctors.
 */
@Service 
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    // Constructor Injection for Dependencies
    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    /**
     * Private helper method to filter a list of doctors by AM/PM availability.
     * Time slots are assumed to be stored as a List<String> of time format "HH:mm" or "HH:mm:ss".
     * @param doctors The list of doctors to filter.
     * @param timePeriod "AM" (before noon) or "PM" (noon and after).
     * @return Filtered list of doctors.
     */
    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String timePeriod) {
        if (timePeriod == null || timePeriod.isEmpty()) return doctors;

        return doctors.stream().filter(doctor ->
            doctor.getAvailableTimes().stream().anyMatch(timeStr -> {
                try {
                    // Check if the time string is a valid format for LocalTime parsing
                    LocalTime time = LocalTime.parse(timeStr);
                    boolean isAM = time.isBefore(LocalTime.NOON);
                    
                    if (timePeriod.equalsIgnoreCase("AM")) {
                        return isAM;
                    } else if (timePeriod.equalsIgnoreCase("PM")) {
                        // PM includes noon (12:00:00) and onward
                        return !isAM;
                    }
                    return false;
                } catch (Exception e) {
                    // Log error if time string format is invalid, but continue filtering
                    System.err.println("Invalid time format in doctor availability: " + timeStr);
                    return false;
                }
            })
        ).collect(Collectors.toList());
    }

    /**
     * Helper to wrap doctor lists into the required Map<String, Object> response format.
     */
    private Map<String, Object> createDoctorMapResponse(List<Doctor> doctors, String key) {
        Map<String, Object> result = new HashMap<>();
        result.put(key, doctors);
        result.put("count", doctors.size());
        return result;
    }
    
    // --- CRUD and Core Logic Methods ---

    /**
     * Fetches the available time slots for a specific doctor on a given date.
     * @param doctorId The ID of the doctor.
     * @param date The date for which availability is needed (LocalDate).
     * @return A list of available time slots (Strings).
     */
    @Transactional
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Optional<Doctor> optionalDoctor = doctorRepository.findById(doctorId);
        if (optionalDoctor.isEmpty()) {
            return Collections.emptyList();
        }

        Doctor doctor = optionalDoctor.get();
        // Assuming getAvailableTimes() returns List<String> of time slots (e.g., "09:00:00")
        List<String> allSlots = doctor.getAvailableTimes(); 

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // Find all booked appointments for the doctor on that day
        List<Appointment> bookedAppointments = appointmentRepository
                // FIX: Changed to findByDoctor_IdAnd...
                .findByDoctor_IdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay);

        // Convert booked appointment times to a set of strings for quick lookup
        Set<String> bookedTimeStrings = bookedAppointments.stream()
                .map(appt -> appt.getAppointmentTime().toLocalTime().toString())
                .collect(Collectors.toSet());

        // Filter out booked slots from the doctor's general available times
        return allSlots.stream()
                .filter(slot -> !bookedTimeStrings.contains(slot))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Saves a new doctor to the database, checking for email conflict.
     * @param doctor The doctor object to save.
     * @return 1 for success, -1 if email exists, 0 for internal error.
     */
    @Transactional
    public int saveDoctor(Doctor doctor) {
        if (doctorRepository.findByEmail(doctor.getEmail()) != null) {
            return -1; // Conflict: Doctor with this email already exists
        }
        try {
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Internal error
        }
    }

    /**
     * Updates the details of an existing doctor.
     * @param doctor The doctor object with updated details (must include ID).
     * @return 1 for success, -1 if doctor not found, 0 for internal error.
     */
    @Transactional
    public int updateDoctor(Doctor doctor) {
        Long id = doctor.getId();
        if (id == null || !doctorRepository.existsById(id)) {
            return -1; // Doctor not found
        }
        
        try {
            // Spring Data JPA save method updates if the ID is present
            doctorRepository.save(doctor); 
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Internal error
        }
    }

    /**
     * Retrieves a list of all doctors.
     * @return A list of all doctors.
     */
    @Transactional
    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }

    /**
     * Deletes a doctor by ID and all associated appointments.
     * @param id The ID of the doctor to be deleted.
     * @return 1 for success, -1 if doctor not found, 0 for internal error.
     */
    @Transactional
    public int deleteDoctor(long id) {
        if (!doctorRepository.existsById(id)) {
            return -1; // Doctor not found
        }
        try {
            // Crucial step: Delete associated appointments first due to foreign key constraints
            // FIX: Changed to deleteAllByDoctor_Id
            appointmentRepository.deleteAllByDoctor_Id(id); 
            doctorRepository.deleteById(id);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Internal error
        }
    }

    /**
     * Validates a doctor's login credentials and generates a JWT token upon success.
     * @param email The doctor's email.
     * @param password The doctor's password.
     * @return ResponseEntity with a token on success or an error message on failure.
     */
    @Transactional
    public ResponseEntity<Map<String, String>> validateDoctor(String email, String password) {
        Map<String, String> response = new HashMap<>();
        Doctor doctor = doctorRepository.findByEmail(email);

        // Simple password check (should be replaced by secure hashing)
        if (doctor == null || !doctor.getPassword().equals(password)) {
            response.put("error", "Invalid email or password");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
        
        // Generate token using the doctor's email as the identifier
        String token = tokenService.generateToken(doctor.getEmail());
        
        response.put("token", token);
        response.put("role", "doctor");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    // --- Filtering Methods ---

    /**
     * Unified method to filter doctors based on optional criteria: name, specialty, and time of day (AM/PM).
     * This method handles all combinations of filtering parameters provided by the DoctorController.
     * @param name Optional partial name of the doctor.
     * @param specialty Optional exact specialty of the doctor.
     * @param amOrPm Optional time period ("AM" or "PM").
     * @return A list of doctors matching the criteria.
     */
    @Transactional
    public List<Doctor> filterDoctors(String name, String specialty, String amOrPm) {
        // Normalize inputs: treat null or blank strings as null
        name = (name != null && !name.isBlank()) ? name.trim() : null;
        specialty = (specialty != null && !specialty.isBlank()) ? specialty.trim() : null;
        amOrPm = (amOrPm != null && !amOrPm.isBlank()) ? amOrPm.trim() : null;

        List<Doctor> doctors;

        // 1. Determine the base list using repository calls (combining name and specialty for efficiency)
        if (name != null && specialty != null) {
            // Find by Name (partial, case-insensitive) AND Specialty (exact, case-insensitive)
            doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);
        } else if (name != null) {
            // Find by Name only (using LIKE %name%)
            // The existing findByNameLike requires us to manually add the wildcards
            doctors = doctorRepository.findByNameLike("%" + name + "%");
        } else if (specialty != null) {
            // Find by Specialty only (exact, case-insensitive)
            doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
        } else {
            // No name or specialty filter provided, start with all doctors
            doctors = doctorRepository.findAll();
        }

        // 2. Apply time filtering if amOrPm is provided (using the existing private helper)
        if (amOrPm != null) {
            doctors = filterDoctorByTime(doctors, amOrPm);
        }

        // 3. Return the final filtered list
        return doctors;
    }


    /**
     * Finds doctors by their name (partial match).
     */
    @Transactional
    public List<Doctor> filterDoctorsByName(String name) {
        // Uses LIKE %name% for searching
        return doctorRepository.findByNameLike("%" + name + "%");
    }
    
    /**
     * Filters doctors by name, specialty, and availability during AM/PM.
     */
    @Transactional
    public List<Doctor> filterDoctorsByNameSpecialtyAndTime(String name, String specialty, String amOrPm) {
        // Can now delegate to the unified method
        return filterDoctors(name, specialty, amOrPm);
    }

    /**
     * Filters doctors by name and their availability during AM/PM.
     */
    @Transactional
    public List<Doctor> filterDoctorsByNameAndTime(String name, String amOrPm) {
        // Can now delegate to the unified method
        return filterDoctors(name, null, amOrPm);
    }

    /**
     * Filters doctors by name and specialty.
     */
    @Transactional
    public List<Doctor> filterDoctorsByNameAndSpecialty(String name, String specialty) {
        // Can now delegate to the unified method
        return filterDoctors(name, specialty, null);
    }

    /**
     * Filters doctors by specialty and their availability during AM/PM.
     */
    @Transactional
    public List<Doctor> filterDoctorsByTimeAndSpecialty(String amOrPm, String specialty) {
        // Can now delegate to the unified method
        return filterDoctors(null, specialty, amOrPm);
    }

    /**
     * Filters doctors by specialty.
     */
    @Transactional
    public List<Doctor> filterDoctorsBySpecialty(String specialty) {
        // Can now delegate to the unified method
        return filterDoctors(null, specialty, null);
    }

    /**
     * Filters all doctors by their availability during AM/PM.
     */
    @Transactional
    public List<Doctor> filterDoctorsByTime(String amOrPm) {
        // Can now delegate to the unified method
        return filterDoctors(null, null, amOrPm);
    } 
}
