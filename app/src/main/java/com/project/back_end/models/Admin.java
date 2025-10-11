package com.project.back_end.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * JPA Entity representing an Admin user in the system.
 * Mapped to the "admin" table.
 */
@Entity
@Table(name = "admin")
public class Admin {

    // 1. 'id' field: Primary key, auto-generated.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 2. 'username' field: Must not be null and must be unique in the database.
    @NotNull(message = "Username cannot be null")
    @Column(nullable = false, unique = true)
    private String username;

    // 3. 'password' field: Must not be null. Hidden from JSON responses.
    @NotNull(message = "Password cannot be null")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // Prevents password from being returned in API responses
    @Column(nullable = false)
    private String password;

    // 4. Default constructor required by JPA
    public Admin() {
    }

    // Parameterized constructor for convenience
    public Admin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // 5. Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
