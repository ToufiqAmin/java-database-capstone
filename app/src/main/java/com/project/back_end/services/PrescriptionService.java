package com.project.back_end.services;

import com.project.back_end.models.Prescription;
import com.project.back_end.repo.PrescriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class for handling business logic related to prescriptions, including 
 * creation and retrieval.
 */
@Service 
public class PrescriptionService {
    
    private final PrescriptionRepository prescriptionRepository;

    // 2. Constructor Injection
    public PrescriptionService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    /**
     * 1. Saves a new prescription to the database, ensuring no existing prescription 
     * is present for the same appointment ID.
     * @param prescription The prescription object to be saved.
     * @return ResponseEntity indicating success (201) or failure (400, 500).
     */
    public ResponseEntity<Map<String, Object>> savePrescription(Prescription prescription) {
        try {
            // Check if a prescription already exists for this appointment
            List<Prescription> existing = prescriptionRepository.findByAppointmentId(prescription.getAppointmentId());

            if (!existing.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Prescription already exists for this appointment.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            prescriptionRepository.save(prescription);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Prescription saved successfully.");
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to save prescription.");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 2. Retrieves the prescription associated with a specific appointment ID.
     * @param appointmentId The appointment ID whose associated prescription is to be retrieved.
     * @return ResponseEntity containing the prescription (200) or an error/not-found message (404, 500).
     */
    public ResponseEntity<Map<String, Object>> getPrescription(Long appointmentId) {
        try {
            List<Prescription> prescriptions = prescriptionRepository.findByAppointmentId(appointmentId);

            if (prescriptions.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "No prescription found for this appointment.");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            // Assuming only one prescription per appointment is allowed, return the first one
            Prescription prescription = prescriptions.get(0);
            Map<String, Object> response = new HashMap<>();
            response.put("prescription", prescription);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error retrieving prescription.");
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
