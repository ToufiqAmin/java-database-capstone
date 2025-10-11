package com.project.back_end.services;

import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Service component responsible for generating, extracting, and validating JWT tokens
 * for secure authentication of Admin, Doctor, and Patient users.
 * FIX: Added public method extractEmailFromToken to resolve 'cannot find symbol' errors in other services.
 */
@Component
public class TokenService {

    // Inject the secret key from application configuration (e.g., application.properties)
    @Value("${jwt.secret}")
    private String secret;

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    // Constructor injection for all necessary repositories
    public TokenService(AdminRepository adminRepository, DoctorRepository doctorRepository, PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Retrieves the HMAC SHA key used for signing JWT tokens from the configured secret.
     * @return The SecretKey used for signing/verification.
     */
    // getSigningKey Method
    private SecretKey getSigningKey() {
        // Generates an HMAC SHA key for the secret
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generates a JWT token for a given user's identifier.
     * The token subject is the unique identifier (username/email), expiring in 7 days.
     *
     * @param identifier The unique identifier (username for Admin, email for Doctor/Patient).
     * @return The generated JWT token string.
     */
    // generateToken Method
    public String generateToken(String identifier) {
        return Jwts.builder()
                .subject(identifier)
                .issuedAt(new Date())
                // Expiration set to 7 days
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7))
                .signWith(getSigningKey()) 
                .compact();
    }

    /**
     * Extracts the identifier (subject) from a JWT token.
     *
     * @param token The JWT token.
     * @return The identifier extracted from the token's subject claim, or null if validation fails.
     */
    // extractIdentifier Method
    public String extractIdentifier(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            // Token is invalid (expired, corrupted, or wrong signature)
            System.err.println("JWT Token Validation/Extraction Failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts the identifier (email/username) from a JWT token.
     * This method is named specifically for clarity and backward compatibility with services 
     * expecting an 'email' extraction method.
     * * @param token The JWT token.
     * @return The identifier (email/username) extracted from the token's subject claim, or null if validation fails.
     */
    public String extractEmailFromToken(String token) {
        return extractIdentifier(token);
    }


    /**
     * Validates the JWT token for a given user type.
     * Checks if the token is valid AND if the user exists in the corresponding repository.
     *
     * @param token The JWT token to be validated.
     * @param user The type of user ("admin", "doctor", or "patient").
     * @return true if the token is valid for the specified user type, false otherwise.
     */
    // validateToken Method
    public boolean validateToken(String token, String user) {
        String identifier = extractIdentifier(token);
        if (identifier == null) {
            return false;
        }

        switch (user.toLowerCase()) {
            case "admin":
                // Admin uses username as identifier
                Admin admin = adminRepository.findByUsername(identifier);
                return admin != null;
            case "doctor":
                // Doctor uses email as identifier
                Doctor doctor = doctorRepository.findByEmail(identifier);
                return doctor != null;
            case "patient":
                // Patient uses email as identifier
                Patient patient = patientRepository.findByEmail(identifier);
                return patient != null;
            default:
                // Unknown user type
                return false;
        }
    }
    
    /**
     * Utility method used by other services (like AppointmentService) to retrieve the primary key 
     * ID of the authorized user (Doctor or Patient) from the token's identifier.
     * * @param token The JWT token.
     * @return The Long ID of the authenticated user, or null if the token is invalid or user not found.
     */
    public Long getUserIdFromToken(String token) {
        String identifier = extractIdentifier(token);
        if (identifier == null) {
            return null;
        }

        // Check if identifier belongs to a Doctor
        Doctor doctor = doctorRepository.findByEmail(identifier);
        if (doctor != null) {
            return doctor.getId();
        }
        
        // Check if identifier belongs to a Patient
        Patient patient = patientRepository.findByEmail(identifier);
        if (patient != null) {
            return patient.getId();
        }

        // Check if identifier belongs to an Admin (by username)
        Admin admin = adminRepository.findByUsername(identifier);
        if (admin != null) {
             // Assuming Admin ID is also Long
            return admin.getId(); 
        }
        
        return null;
    }
}
