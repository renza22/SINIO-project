package com.sinio.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmployeeRequest {

    private Long id;

    @NotBlank(message = "Nama lengkap wajib diisi.")
    @Size(max = 120, message = "Nama lengkap maksimal 120 karakter.")
    private String fullName;

    @NotBlank(message = "Email wajib diisi.")
    @Email(message = "Format email tidak valid.")
    @Size(max = 120, message = "Email maksimal 120 karakter.")
    private String email;

    private String password;
    private String confirmPassword;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public boolean isNew() {
        return id == null;
    }
}
