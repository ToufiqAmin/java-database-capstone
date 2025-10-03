package com.project.back_end.DTO;

import com.project.back_end.models.Doctor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DoctorDTO {
    private Long id;
    private String name;
    private String specialty;
    private String email;
    private String phone;
    private List<String> availableTimes;

    // Constructor to map from Doctor entity
    public DoctorDTO(Doctor doctor) {
        this.id = doctor.getId();
        this.name = doctor.getName();
        this.specialty = doctor.getSpecialty();
        this.email = doctor.getEmail();
        this.phone = doctor.getPhone();
        this.availableTimes = doctor.getAvailableTimes() != null
            ? new ArrayList<>(doctor.getAvailableTimes())
            : Collections.emptyList();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<String> getAvailableTimes() {
        return availableTimes;
    }

    public void setAvailableTimes(List<String> availableTimes) {
        this.availableTimes = availableTimes;
    }
}