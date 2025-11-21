package com.sinio.demo.service;

import com.sinio.demo.dto.EmployeeRequest;
import com.sinio.demo.dto.EmployeeRoleOption;
import com.sinio.demo.dto.EmployeeView;
import com.sinio.demo.dto.RegisterRequest;
import com.sinio.demo.model.Guest;
import com.sinio.demo.model.Karyawan;
import com.sinio.demo.model.Role;
import com.sinio.demo.model.User;
import com.sinio.demo.model.UserRole;
import com.sinio.demo.repository.GuestRepository;
import com.sinio.demo.repository.KaryawanRepository;
import com.sinio.demo.repository.RoleRepository;
import com.sinio.demo.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final Map<String, String> ROLE_LABELS = Map.of(
        "RESEPSIONIS", "Resepsionis",
        "HOUSEKEEPING", "Housekeeping",
        "KASIR", "Kasir"
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GuestRepository guestRepository;
    private final KaryawanRepository karyawanRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
        UserRepository userRepository,
        GuestRepository guestRepository,
        RoleRepository roleRepository,
        KaryawanRepository karyawanRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.guestRepository = guestRepository;
        this.roleRepository = roleRepository;
        this.karyawanRepository = karyawanRepository;
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

        User saved = userRepository.save(user);
        ensureGuestProfile(saved);
        return saved;
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
            User saved = userRepository.save(user);
            ensureGuestProfile(saved);
            return saved;
        }
        ensureGuestProfile(user);
        return user;
    }

    @Transactional(readOnly = true)
    public List<EmployeeView> findAllEmployees() {
        List<User> users = userRepository.findAllByRoleOrderByFullNameAsc(UserRole.KARYAWAN);
        return users.stream().map(this::toView).toList();
    }

    @Transactional
    public User createEmployee(EmployeeRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        ensureEmailAvailable(normalizedEmail);
        String password = requireValidPassword(request, true);
        Set<Role> roles = resolveRoles(request.getRoleCodes());

        User employee = new User();
        employee.setFullName(request.getFullName().trim());
        employee.setEmail(normalizedEmail);
        employee.setPasswordHash(passwordEncoder.encode(password));
        employee.setRole(UserRole.KARYAWAN);

        User saved = userRepository.save(employee);
        upsertKaryawan(saved, roles);
        return saved;
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

        User saved = userRepository.save(employee);
        Set<Role> roles = resolveRoles(request.getRoleCodes());
        upsertKaryawan(saved, roles);
        return saved;
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

        karyawanRepository.findByUserId(id).ifPresent(karyawanRepository::delete);
        userRepository.delete(employee);
    }

    @Transactional
    public User updateGuestProfile(Long userId, String fullName, String email) {
        if (userId == null) {
            throw new IllegalArgumentException("Pengguna tidak dikenal.");
        }
        User user = userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan."));

        String normalizedEmail = normalizeEmail(email);
        ensureEmailAvailableForUpdate(normalizedEmail, userId);

        user.setFullName(fullName == null ? null : fullName.trim());
        user.setEmail(normalizedEmail);
        return userRepository.save(user);
    }

    @Transactional
    public void changeGuestPassword(Long userId, String currentPassword, String newPassword) {
        if (userId == null) {
            throw new IllegalArgumentException("Pengguna tidak dikenal.");
        }
        User user = userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan."));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Password saat ini tidak sesuai.");
        }

        if (!StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("Password baru wajib diisi.");
        }

        String trimmed = newPassword.trim();
        if (trimmed.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password baru minimal " + MIN_PASSWORD_LENGTH + " karakter.");
        }

        if (passwordEncoder.matches(trimmed, user.getPasswordHash())) {
            throw new IllegalArgumentException("Password baru tidak boleh sama dengan password lama.");
        }

        user.setPasswordHash(passwordEncoder.encode(trimmed));
        userRepository.save(user);
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

    @Transactional(readOnly = true)
    public List<EmployeeRoleOption> getRoleOptions() {
        return ROLE_LABELS.entrySet().stream()
            .map(e -> new EmployeeRoleOption(e.getKey(), e.getValue()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getEmployeeRoleCodes(Long userId) {
        return karyawanRepository.findByUserId(userId)
            .map(k -> k.getRoles().stream()
                .map(Role::getCode)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .toList())
            .orElseGet(List::of);
    }

    private Set<Role> resolveRoles(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new IllegalArgumentException("Minimal satu peran karyawan harus dipilih.");
        }
        Set<String> normalized = codes.stream()
            .filter(StringUtils::hasText)
            .map(c -> c.trim().toUpperCase(Locale.ROOT))
            .collect(Collectors.toSet());
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Minimal satu peran karyawan harus dipilih.");
        }

        List<Role> existing = roleRepository.findByCodeIn(normalized);
        Set<String> foundCodes = existing.stream().map(Role::getCode).collect(Collectors.toSet());

        normalized.stream()
            .filter(code -> !foundCodes.contains(code))
            .forEach(code -> {
                Role role = new Role();
                role.setCode(code);
                role.setName(ROLE_LABELS.getOrDefault(code, code));
                roleRepository.save(role);
                existing.add(role);
            });

        return existing.stream().collect(Collectors.toSet());
    }

    private EmployeeView toView(User user) {
        EmployeeView view = new EmployeeView();
        view.setId(user.getId());
        view.setFullName(user.getFullName());
        view.setEmail(user.getEmail());
        view.setCreatedAt(user.getCreatedAt());
        List<String> codes = getEmployeeRoleCodes(user.getId());
        view.setRoleCodes(codes);
        view.setRoles(codes.stream().map(code -> ROLE_LABELS.getOrDefault(code, code)).toList());
        return view;
    }

    private void upsertKaryawan(User user, Set<Role> roles) {
        if (user == null) {
            return;
        }
        Karyawan karyawan = karyawanRepository.findByUserId(user.getId()).orElseGet(Karyawan::new);
        karyawan.setUser(user);
        karyawan.setRoles(roles);
        karyawanRepository.save(karyawan);
    }

    private void ensureGuestProfile(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        guestRepository.findByUser_Id(user.getId()).orElseGet(() -> {
            Guest guest = new Guest();
            guest.setUser(user);
            return guestRepository.save(guest);
        });
    }
}
