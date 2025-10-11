package com.project.back_end.DTO;

/**
 * Data Transfer Object (DTO) for handling user login requests.
 * This class encapsulates the credentials submitted by the client 
 * and is used for deserializing the request body.
 */
public class Login {
    
    // The unique identifier of the user (email for Patient/Doctor, username for Admin)
    private String identifier;
    
    // The password provided by the user
    private String password;

    // Default constructor.g FIX: Must match class name 'Login'.
    public Login() {
    }

    // Parameterized constructor. FIX: Must match class name 'Login'.
    public Login(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }

    // --- Standard Getter Methods ---

    /**
     * @return The unique identifier (email/username) for login.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the unique identifier for login.
     * @param identifier The user's identifier.
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return The password provided by the user.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password for login.
     * @param password The user's password.
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
