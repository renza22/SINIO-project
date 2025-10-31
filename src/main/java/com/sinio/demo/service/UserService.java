package com.sinio.demo.service;

import com.sinio.demo.dto.EmployeeRequest;
import com.sinio.demo.dto.RegisterRequest;
import com.sinio.demo.model.User;
import com.sinio.demo.model.UserRole;
import com.sinio.demo.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email sudah terdaftar. Silakan gunakan email lain.");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.TAMU);

        return userRepository.save(user);
    }

    public Optional<User> authenticate(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository
            .findByEmail(normalizedEmail)
            .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()));
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User ensureRole(User user) {
        if (user.getRole() == null) {
            user.setRole(UserRole.TAMU);
            return userRepository.save(user);
        }
        return user;
    }

    @Transactional(readOnly = true)
    public List<User> findAllEmployees() {
        return userRepository.findAllByRoleOrderByFullNameAsc(UserRole.KARYAWAN);
    }

    @Transactional
    public User createEmployee(EmployeeRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        ensureEmailAvailable(normalizedEmail);
        String password = requireValidPassword(request, true);

        User employee = new User();
        employee.setFullName(request.getFullName().trim());
        employee.setEmail(normalizedEmail);
        employee.setPasswordHash(passwordEncoder.encode(password));
        employee.setRole(UserRole.KARYAWAN);

        return userRepository.save(employee);
    }

    @Transactional
    public User updateEmployee(EmployeeRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("ID karyawan tidak ditemukan.");
        }

        User employee = userRepository
            .findById(request.getId())
            .orElseThrow(() -> new IllegalArgumentException("Karyawan tidak ditemukan."));

        if (employee.getRole() != UserRole.KARYAWAN) {
            throw new IllegalArgumentException("Hanya karyawan yang dapat diperbarui.");
        }

        String normalizedEmail = normalizeEmail(request.getEmail());
        ensureEmailAvailableForUpdate(normalizedEmail, employee.getId());

        employee.setFullName(request.getFullName().trim());
        employee.setEmail(normalizedEmail);

        String password = requireValidPassword(request, false);
        if (password != null) {
            employee.setPasswordHash(passwordEncoder.encode(password));
        }

        return userRepository.save(employee);
    }

    @Transactional
    public void deleteEmployee(Long id, Long currentUserId) {
        User employee = userRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Karyawan tidak ditemukan."));

        if (employee.getRole() != UserRole.KARYAWAN) {
            throw new IllegalArgumentException("Pengguna ini bukan karyawan.");
        }

        if (currentUserId != null && currentUserId.equals(id)) {
            throw new IllegalArgumentException("Anda tidak dapat menghapus akun sendiri.");
        }

        userRepository.delete(employee);
    }

    private void ensureEmailAvailable(String normalizedEmail) {
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email sudah terdaftar. Silakan gunakan email lain.");
        }
    }

    private void ensureEmailAvailableForUpdate(String normalizedEmail, Long id) {
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new IllegalArgumentException("Email sudah digunakan oleh pengguna lain.");
        }
    }

    private String requireValidPassword(EmployeeRequest request, boolean required) {
        String rawPassword = request.getPassword();
        String confirmPassword = request.getConfirmPassword();

        if (!StringUtils.hasText(rawPassword)) {
            if (required) {
                throw new IllegalArgumentException("Password wajib diisi.");
            }
            return null;
        }

        rawPassword = rawPassword.trim();
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                "Password minimal " + MIN_PASSWORD_LENGTH + " karakter."
            );
        }

        String confirmation = StringUtils.hasText(confirmPassword) ? confirmPassword.trim() : "";
        if (!rawPassword.equals(confirmation)) {
            throw new IllegalArgumentException("Konfirmasi password tidak cocok.");
        }

        return rawPassword;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
