package com.project.back_end.controllers;

import com.project.back_end.models.Admin;
import com.project.back_end.services.CentralService; // Import the service class correctly
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for handling administrative operations, primarily login/authentication.
 */
@RestController // 1. Designates this class as a REST controller
@RequestMapping("${api.path}admin") // Sets the base path for all endpoints in this controller
public class AdminController {

    private final CentralService service; // Use the correct class name 'CentralService'

    // 2. Constructor-based dependency injection
    public AdminController(CentralService service) {
        this.service = service;
    }

    /**
     * 3. Handles admin login requests by validating credentials.
     * Endpoint: POST /api/admin/login
     * @param admin The Admin object containing username and password from the request body.
     * @return ResponseEntity containing a token on success (200 OK) or an error message (401 UNAUTHORIZED/500 INTERNAL SERVER ERROR).
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> adminLogin(@RequestBody Admin admin) {
        // Delegate validation to the service layer.
        // The service layer's validateAdmin implementation in the Canvas expects an Admin object.
        // Re-aligning the service call to match the updated Service implementation in the canvas
        return service.validateAdmin(admin);
    }
}
