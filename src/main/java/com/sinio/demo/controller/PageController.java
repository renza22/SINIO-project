package com.sinio.demo.controller;

import com.sinio.demo.dto.EmployeeRequest;
import com.sinio.demo.dto.EmployeeRoleOption;
import com.sinio.demo.dto.EmployeeView;
import com.sinio.demo.dto.GuestPasswordForm;
import com.sinio.demo.dto.GuestProfileForm;
import com.sinio.demo.dto.LoginRequest;
import com.sinio.demo.dto.RegisterRequest;
import com.sinio.demo.dto.RoomRequest;
import com.sinio.demo.dto.ReservationRequest;
import com.sinio.demo.dto.RoomSummaryView;
import com.sinio.demo.model.Reservation;
import com.sinio.demo.model.Room;
import com.sinio.demo.model.Payment;
import com.sinio.demo.model.PaymentStatus;
import com.sinio.demo.model.User;
import com.sinio.demo.model.UserRole;
import com.sinio.demo.model.RoomStatus;
import com.sinio.demo.model.RoomType;
import com.sinio.demo.service.RoomService;
import com.sinio.demo.service.ReservationService;
import com.sinio.demo.service.UserService;
import com.sinio.demo.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Controller
public class PageController {

    private final UserService userService;
    private final RoomService roomService;
    private final ReservationService reservationService;
    private final PaymentService paymentService;

    public PageController(UserService userService, RoomService roomService, ReservationService reservationService, PaymentService paymentService) {
        this.userService = userService;
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.paymentService = paymentService;
    }

    @GetMapping("/")
    public String landing(Model model) {
        List<RoomSummaryView> featured = roomService.findFeaturedSummaries(3);
        model.addAttribute("featuredRooms", featured);
        model.addAttribute("hasRooms", !featured.isEmpty());
        return "landing";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        if (!model.containsAttribute("activePanel")) {
            model.addAttribute("activePanel", "login");
        }
        return "sinio_login";
    }

    @PostMapping("/register")
    public String handleRegister(
        @Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerRequest", bindingResult);
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            redirectAttributes.addFlashAttribute("activePanel", "register");
            return "redirect:/login";
        }

        try {
            userService.registerUser(registerRequest);
            redirectAttributes.addFlashAttribute("loginMessage", "Akun berhasil dibuat. Silakan login.");
            redirectAttributes.addFlashAttribute("activePanel", "login");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("registerError", ex.getMessage());
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            redirectAttributes.addFlashAttribute("activePanel", "register");
        }
        return "redirect:/login";
    }

    @PostMapping("/login")
    public String handleLogin(
        @Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
        BindingResult bindingResult,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.loginRequest", bindingResult);
            redirectAttributes.addFlashAttribute("loginRequest", loginRequest);
            redirectAttributes.addFlashAttribute("activePanel", "login");
            return "redirect:/login";
        }

        return userService
            .authenticate(loginRequest.getEmail(), loginRequest.getPassword())
            .map(user -> onLoginSuccess(user, session))
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("loginError", "Email atau password salah.");
                redirectAttributes.addFlashAttribute("loginRequest", loginRequest);
                redirectAttributes.addFlashAttribute("activePanel", "login");
                return "redirect:/login";
            });
    }

    private String onLoginSuccess(User user, HttpSession session) {
        User ensuredUser = userService.ensureRole(user);
        session.setAttribute("userId", ensuredUser.getId());
        session.setAttribute("userName", ensuredUser.getFullName());
        session.setAttribute("userEmail", ensuredUser.getEmail());
        UserRole role = ensuredUser.getRole() != null ? ensuredUser.getRole() : UserRole.TAMU;
        session.setAttribute("userRole", role);
        if (role == UserRole.KARYAWAN) {
            session.setAttribute("userRoles", userService.getEmployeeRoleCodes(ensuredUser.getId()));
        } else {
            session.setAttribute("userRoles", java.util.Collections.emptyList());
        }
        return redirectForRole(role);
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, RedirectAttributes redirectAttributes) {
        UserRole role = resolveUserRole(session);
        if (role == null) {
            session.invalidate();
            redirectAttributes.addFlashAttribute("loginError", "Silakan login terlebih dahulu.");
            return "redirect:/login";
        }
        return switch (role) {
            case ADMIN -> "redirect:/dashboard/admin";
            case KARYAWAN -> "redirect:/dashboard/karyawan";
            default -> "redirect:/dashboard/tamu";
        };
    }

    @GetMapping("/dashboard/tamu")
    public String guestDashboard(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        return renderDashboardForRole(session, redirectAttributes, model, UserRole.TAMU, "dashboard_tamu");
    }

    // Alias path for guest home
    @GetMapping("/guest")
    public String guestHome(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        return renderDashboardForRole(session, redirectAttributes, model, UserRole.TAMU, "dashboard_tamu");
    }

    // ---- Guest: browse rooms ----
    @GetMapping("/guest/kamar")
    public String guestBrowseRooms(
        HttpSession session,
        RedirectAttributes redirectAttributes,
        Model model,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "12") int size
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.TAMU);
        if (redirect != null) {
            return redirect;
        }
        populateCommonModel(session, model);
        Page<RoomSummaryView> roomsPage = roomService.findGuestSummaries(page, size);
        model.addAttribute("rooms", roomsPage.getContent());
        model.addAttribute("page", roomsPage.getNumber());
        model.addAttribute("pageSize", roomsPage.getSize());
        model.addAttribute("totalPages", roomsPage.getTotalPages());
        model.addAttribute("hasPrev", roomsPage.hasPrevious());
        model.addAttribute("hasNext", roomsPage.hasNext());
        model.addAttribute("roomTypes", roomService.getRoomTypes());
        return "guest_kamar";
    }

    @GetMapping("/guest/kamar/{id}")
    public String guestRoomDetail(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        String redirect = guardRole(session, redirectAttributes, UserRole.TAMU);
        if (redirect != null) {
            return redirect;
        }
        return roomService.findById(id)
            .map(room -> {
                populateCommonModel(session, model);
                model.addAttribute("room", room);
                model.addAttribute("amenities", roomService.resolveAmenities(room));
                model.addAttribute("services", roomService.resolveServiceOptions(room));
                return "guest_kamar_detail";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("guestError", "Kamar tidak ditemukan.");
                return "redirect:/guest/kamar";
            });
    }

    // ---- Guest: reservations (placeholder list) ----
    @GetMapping("/guest/reservasi")
    public String guestReservations(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        String redirect = guardRole(session, redirectAttributes, UserRole.TAMU);
        if (redirect != null) {
            return redirect;
        }
        populateCommonModel(session, model);
        try {
            Long userId = (Long) session.getAttribute("userId");
            var reservations = reservationService.findByUser(userId);
            List<Map<String, Object>> views = reservations.stream()
                .map(r -> {
                    Room room = reservationService.safePrimaryRoom(r);
                    if (room == null) {
                        return null; // skip reservasi tanpa kamar (data kotor)
                    }
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", r.getId());
                    m.put("kode", r.getCode());
                    m.put("nomorKamar", room.getNumber());
                    m.put("tipe", room.getType().getDisplayName());
                    m.put("periode", DateTimeFormatter.ofPattern("dd MMM yyyy").format(r.getCheckIn()) + " - " + DateTimeFormatter.ofPattern("dd MMM yyyy").format(r.getCheckOut()));
                    m.put("status", r.getStatus().name());
                    Payment latest = paymentService.getLatestPaymentForReservation(r.getId());
                    boolean isOwnReservation = Objects.equals(r.getUser().getId(), userId);
                    if (latest != null && latest.getStatus() == PaymentStatus.PENDING && isOwnReservation) {
                        boolean isCash = "CASH".equalsIgnoreCase(latest.getPaymentType());
                        m.put("displayStatus", isCash ? "MENUNGGU PEMBAYARAN CASH" : "MENUNGGU PEMBAYARAN ONLINE");
                        m.put("pendingOnline", !isCash);
                    } else {
                        m.put("displayStatus", r.getStatus().name());
                        m.put("pendingOnline", false);
                    }
                    return m;
                })
                .filter(Objects::nonNull)
                .toList();
            model.addAttribute("reservations", views);
        } catch (Exception ex) {
            model.addAttribute("reservations", java.util.Collections.emptyList());
            model.addAttribute("guestError", "Gagal memuat reservasi: " + ex.getMessage());
        }
        return "guest_reservasi";
    }

    @GetMapping("/guest/tagihan")
    public String guestBilling(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        String redirect = guardRole(session, redirectAttributes, UserRole.TAMU);
        if (redirect != null) {
            return redirect;
        }
        populateCommonModel(session, model);
        Long userId = requireUserId(session, redirectAttributes);
        if (userId == null) {
            return "redirect:/login";
        }
        List<Map<String, Object>> bills = paymentService.getPendingBillsForUser(userId);
        java.math.BigDecimal totalDue = bills.stream()
            .map(b -> (java.math.BigDecimal) b.getOrDefault("amount", java.math.BigDecimal.ZERO))
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        model.addAttribute("bills", bills);
        model.addAttribute("totalDue", totalDue);
        model.addAttribute("midtransClientKey", paymentService.getClientKey());
        return "guest_tagihan";
    }

    @GetMapping("/guest/layanan")
    public String guestServices(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        return guestDashboard(session, redirectAttributes, model);
    }

    // ---- Guest: create reservation ----
    @PostMapping("/guest/reservasi")
    public Object createReservation(
        @Valid @ModelAttribute("reservationRequest") ReservationRequest reservationRequest,
        BindingResult bindingResult,
        HttpSession session,
        RedirectAttributes redirectAttributes,
        HttpServletRequest httpRequest
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.TAMU);
        if (redirect != null) {
            return redirect;
        }
        Long userId = (Long) session.getAttribute("userId");

        if (bindingResult.hasErrors()) {
            if (acceptsJson(httpRequest)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Input reservasi tidak valid."
                ));
            }
            redirectAttributes.addFlashAttribute("guestError", "Input reservasi tidak valid.");
            return "redirect:/guest/kamar/" + reservationRequest.getRoomId();
        }

        try {
            Reservation reservation = reservationService.create(userId, reservationRequest);
            if (acceptsJson(httpRequest)) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "reservationId", reservation.getId(),
                    "redirect", "/guest/tagihan"
                ));
            }
            redirectAttributes.addFlashAttribute("guestMessage", "Reservasi dicatat. Lanjutkan pembayaran melalui menu Tagihan & Pembayaran.");
            return "redirect:/guest/tagihan";
        } catch (IllegalArgumentException ex) {
            if (acceptsJson(httpRequest)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
                ));
            }
            redirectAttributes.addFlashAttribute("guestError", ex.getMessage());
            return "redirect:/guest/kamar/" + reservationRequest.getRoomId();
        }
    }

    // ---- Guest: reservation detail ----
    @GetMapping("/guest/reservasi/{id}")
    public String reservationDetail(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        String redirect = guardRole(session, redirectAttributes, UserRole.TAMU);
        if (redirect != null) {
            return redirect;
        }
        Long userId = (Long) session.getAttribute("userId");
        return reservationService
            .findByUser(userId)
            .stream()
            .filter(r -> r.getId().equals(id))
            .findFirst()
            .map(r -> {
                populateCommonModel(session, model);
                model.addAttribute("reservation", r);
                Payment payment = paymentService.getLatestPaymentForReservation(r.getId());
                model.addAttribute("payment", payment);
                model.addAttribute("payments", paymentService.getPaymentsByReservationId(r.getId()));
                model.addAttribute("midtransClientKey", paymentService.getClientKey());
                return "guest_reservasi_detail";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("guestError", "Reservasi tidak ditemukan.");
                return "redirect:/guest/reservasi";
            });
    }

    // ---- Guest: cancel reservation ----
    @PostMapping("/guest/reservasi/{id}/cancel")
    public String cancelReservation(
        @PathVariable Long id,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.TAMU);
        if (redirect != null) {
            return redirect;
        }
        Long userId = (Long) session.getAttribute("userId");
        try {
            reservationService.cancel(userId, id);
            redirectAttributes.addFlashAttribute("guestMessage", "Reservasi berhasil dibatalkan.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("guestError", ex.getMessage());
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("guestError", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("guestError", "Gagal membatalkan reservasi.");
        }
        return "redirect:/guest/reservasi";
    }

    @PostMapping("/guest/layanan/pesan")
    public String orderRoomService(
        @ModelAttribute("layananForm") LayananForm layananForm,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.TAMU);
        if (redirect != null) {
            return redirect;
        }
        Long userId = requireUserId(session, redirectAttributes);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            if (layananForm.getLayananId() == null) {
                throw new IllegalArgumentException("Pilih layanan yang ingin dipesan.");
            }
            int qty = layananForm.getQty() != null ? layananForm.getQty() : 1;
            reservationService.addServiceToActiveReservation(userId, layananForm.getLayananId(), qty);
            redirectAttributes.addFlashAttribute("guestMessage", "Pesanan layanan ditambahkan.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("guestError", ex.getMessage());
            redirectAttributes.addFlashAttribute("layananForm", layananForm);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("guestError", "Gagal menambahkan layanan.");
            redirectAttributes.addFlashAttribute("layananForm", layananForm);
        }
        return "redirect:/dashboard/tamu";
    }

    @PostMapping("/guest/layanan/submit")
    public String submitServiceCart(HttpSession session, RedirectAttributes redirectAttributes) {
        String redirect = guardRole(session, redirectAttributes, UserRole.TAMU);
        if (redirect != null) {
            return redirect;
        }
        Long userId = requireUserId(session, redirectAttributes);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            reservationService.findActiveForUser(userId)
                .orElseThrow(() -> new IllegalStateException("Tidak ada reservasi aktif."));
            redirectAttributes.addFlashAttribute("guestMessage", "Pesanan layanan Anda telah dicatat. Tim kami akan memproses.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("guestError", ex.getMessage());
        }
        return "redirect:/dashboard/tamu";
    }

    @GetMapping("/guest/akun")
    public String guestAccount(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        String redirect = guardRole(session, redirectAttributes, UserRole.TAMU);
        if (redirect != null) {
            return redirect;
        }

        User user = requireUser(session, redirectAttributes);
        if (user == null) {
            return "redirect:/login";
        }

        populateCommonModel(session, model);
        model.addAttribute("accountUser", user);

        if (!model.containsAttribute("profileForm")) {
            GuestProfileForm profileForm = new GuestProfileForm();
            profileForm.setFullName(user.getFullName());
            profileForm.setEmail(user.getEmail());
            model.addAttribute("profileForm", profileForm);
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new GuestPasswordForm());
        }
        return "guest_akun";
    }

    @PostMapping("/guest/akun/profile")
    public String updateGuestProfile(
        @Valid @ModelAttribute("profileForm") GuestProfileForm profileForm,
        BindingResult bindingResult,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.TAMU);
        if (redirect != null) {
            return redirect;
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileForm", bindingResult);
            redirectAttributes.addFlashAttribute("profileForm", profileForm);
            redirectAttributes.addFlashAttribute("focusTarget", "profile");
            return "redirect:/guest/akun";
        }

        Long userId = requireUserId(session, redirectAttributes);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            User updated = userService.updateGuestProfile(userId, profileForm.getFullName(), profileForm.getEmail());
            session.setAttribute("userName", updated.getFullName());
            session.setAttribute("userEmail", updated.getEmail());
            redirectAttributes.addFlashAttribute("profileSuccess", "Profil berhasil diperbarui.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("profileError", ex.getMessage());
            redirectAttributes.addFlashAttribute("profileForm", profileForm);
        }
        redirectAttributes.addFlashAttribute("focusTarget", "profile");
        return "redirect:/guest/akun";
    }

    @PostMapping("/guest/akun/password")
    public String updateGuestPassword(
        @Valid @ModelAttribute("passwordForm") GuestPasswordForm passwordForm,
        BindingResult bindingResult,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.TAMU);
        if (redirect != null) {
            return redirect;
        }

        if (passwordForm.getNewPassword() != null
            && passwordForm.getConfirmPassword() != null
            && !passwordForm.getNewPassword().equals(passwordForm.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Konfirmasi password tidak cocok.");
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.passwordForm", bindingResult);
            redirectAttributes.addFlashAttribute("passwordForm", passwordForm);
            redirectAttributes.addFlashAttribute("focusTarget", "password");
            return "redirect:/guest/akun";
        }

        Long userId = requireUserId(session, redirectAttributes);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            userService.changeGuestPassword(userId, passwordForm.getCurrentPassword(), passwordForm.getNewPassword());
            redirectAttributes.addFlashAttribute("passwordSuccess", "Password berhasil diperbarui.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("passwordError", ex.getMessage());
        }
        redirectAttributes.addFlashAttribute("focusTarget", "password");
        return "redirect:/guest/akun";
    }

    @GetMapping("/dashboard/admin")
    public String adminDashboard(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        return renderDashboardForRole(session, redirectAttributes, model, UserRole.ADMIN, "dashboard_admin");
    }

    @GetMapping("/dashboard/karyawan")
    public String employeeDashboard(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        return renderDashboardForRole(session, redirectAttributes, model, UserRole.KARYAWAN, "dashboard_karyawan");
    }

    @GetMapping("/admin/karyawan")
    public String manageEmployees(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }

        populateCommonModel(session, model);
        List<EmployeeView> employees = userService.findAllEmployees();
        model.addAttribute("employees", employees);
        model.addAttribute("roleOptions", userService.getRoleOptions());

        if (!model.containsAttribute("createForm")) {
            EmployeeRequest create = new EmployeeRequest();
            create.setRoleCodes(Set.of("RESEPSIONIS"));
            model.addAttribute("createForm", create);
        }
        if (!model.containsAttribute("editForm")) {
            EmployeeRequest edit = new EmployeeRequest();
            edit.setRoleCodes(Set.of("RESEPSIONIS"));
            model.addAttribute("editForm", edit);
        }
        if (!model.containsAttribute("formMode")) {
            model.addAttribute("formMode", "create");
        }
        return "admin_crud_karyawan";
    }

    @PostMapping("/admin/kamar")
    public String createRoom(
        @Valid @ModelAttribute("roomForm") RoomRequest roomForm,
        BindingResult bindingResult,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.roomForm", bindingResult);
            redirectAttributes.addFlashAttribute("roomForm", roomForm);
            redirectAttributes.addFlashAttribute("roomFormMode", "create");
            return "redirect:/admin/kamar";
        }

        try {
            roomService.createRoom(roomForm);
            redirectAttributes.addFlashAttribute("roomSuccess", "Kamar baru berhasil ditambahkan.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("roomError", ex.getMessage());
            redirectAttributes.addFlashAttribute("roomForm", roomForm);
            redirectAttributes.addFlashAttribute("roomFormMode", "create");
        }
        return "redirect:/admin/kamar";
    }

    @PostMapping("/admin/kamar/{id}/update")
    public String updateRoom(
        @PathVariable Long id,
        @Valid @ModelAttribute("roomEditForm") RoomRequest roomEditForm,
        BindingResult bindingResult,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }

        roomEditForm.setId(id);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.roomEditForm", bindingResult);
            redirectAttributes.addFlashAttribute("roomEditForm", roomEditForm);
            redirectAttributes.addFlashAttribute("roomFormMode", "edit");
            redirectAttributes.addFlashAttribute("editingRoomId", id);
            return "redirect:/admin/kamar";
        }

        try {
            roomService.updateRoom(id, roomEditForm);
            redirectAttributes.addFlashAttribute("roomFormMode", "create");
            redirectAttributes.addFlashAttribute("roomSuccess", "Data kamar berhasil diperbarui.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("roomError", ex.getMessage());
            redirectAttributes.addFlashAttribute("roomEditForm", roomEditForm);
            redirectAttributes.addFlashAttribute("roomFormMode", "edit");
            redirectAttributes.addFlashAttribute("editingRoomId", id);
        }
        return "redirect:/admin/kamar";
    }

    @PostMapping("/admin/kamar/{id}/delete")
    public String deleteRoom(
        @PathVariable Long id,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }

        try {
            roomService.deleteRoom(id);
            redirectAttributes.addFlashAttribute("roomSuccess", "Kamar berhasil dihapus.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("roomError", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("roomError", "Gagal menghapus kamar.");
        }
        return "redirect:/admin/kamar";
    }

    @GetMapping("/admin/kamar")
    public String manageRooms(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }

        populateCommonModel(session, model);
        List<Room> rooms = roomService.findAllSorted();
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomStats", roomService.buildStats(rooms));
        model.addAttribute("roomActivities", roomService.buildActivities(rooms));
        model.addAttribute("roomTypes", roomService.getRoomTypes());
        model.addAttribute("roomStatuses", roomService.getRoomStatuses());
        model.addAttribute("facilityOptions", roomService.getFacilityOptions());

        if (!model.containsAttribute("roomForm")) {
            RoomRequest create = new RoomRequest();
            create.setMaxOccupancy(2);
            model.addAttribute("roomForm", create);
        }
        if (!model.containsAttribute("roomEditForm")) {
            RoomRequest edit = new RoomRequest();
            edit.setMaxOccupancy(2);
            model.addAttribute("roomEditForm", edit);
        }
        if (!model.containsAttribute("roomFormMode")) {
            model.addAttribute("roomFormMode", "create");
        }
        return "admin_crud_kamar";
    }

    @PostMapping("/admin/karyawan")
    public String createEmployee(
        @Valid @ModelAttribute("createForm") EmployeeRequest createForm,
        BindingResult bindingResult,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.createForm", bindingResult);
            redirectAttributes.addFlashAttribute("createForm", createForm);
            redirectAttributes.addFlashAttribute("formMode", "create");
            return "redirect:/admin/karyawan";
        }

        try {
            createForm.setId(null);
            userService.createEmployee(createForm);
            redirectAttributes.addFlashAttribute("employeeSuccess", "Karyawan berhasil ditambahkan.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("employeeError", ex.getMessage());
            redirectAttributes.addFlashAttribute("createForm", createForm);
            redirectAttributes.addFlashAttribute("formMode", "create");
        }
        return "redirect:/admin/karyawan";
    }

    @PostMapping("/admin/karyawan/{id}/update")
    public String updateEmployee(
        @PathVariable Long id,
        @Valid @ModelAttribute("editForm") EmployeeRequest editForm,
        BindingResult bindingResult,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }

        editForm.setId(id);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.editForm", bindingResult);
            redirectAttributes.addFlashAttribute("editForm", editForm);
            redirectAttributes.addFlashAttribute("formMode", "edit");
            redirectAttributes.addFlashAttribute("editingId", id);
            return "redirect:/admin/karyawan";
        }

        try {
            userService.updateEmployee(editForm);
            redirectAttributes.addFlashAttribute("formMode", "create");
            redirectAttributes.addFlashAttribute("employeeSuccess", "Data karyawan berhasil diperbarui.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("employeeError", ex.getMessage());
            redirectAttributes.addFlashAttribute("editForm", editForm);
            redirectAttributes.addFlashAttribute("formMode", "edit");
            redirectAttributes.addFlashAttribute("editingId", id);
        }
        return "redirect:/admin/karyawan";
    }

    @PostMapping("/admin/karyawan/{id}/delete")
    public String deleteEmployee(
        @PathVariable Long id,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }

        Long currentUserId = (Long) session.getAttribute("userId");
        try {
            userService.deleteEmployee(id, currentUserId);
            redirectAttributes.addFlashAttribute("employeeSuccess", "Karyawan berhasil dihapus.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("employeeError", ex.getMessage());
        }
        return "redirect:/admin/karyawan";
    }

    @GetMapping("/admin/layanan")
    public String manageServices(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }
        populateCommonModel(session, model);
        List<Map<String, Object>> rooms = roomService.findAllSorted().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("nomor", r.getNumber());
            m.put("tipe", r.getType().getDisplayName());
            m.put("layanan", roomService.findServiceOptionsForRoom(r.getId()));
            return m;
        }).toList();
        model.addAttribute("roomsWithServices", rooms);
        return "admin_layanan";
    }

    @PostMapping("/admin/layanan")
    public String addServiceOption(
        @RequestParam Long roomId,
        @RequestParam String nama,
        @RequestParam(required = false) String satuan,
        @RequestParam(required = false) String harga,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }
        try {
            java.math.BigDecimal price = (harga != null && !harga.isBlank()) ? new java.math.BigDecimal(harga.trim()) : java.math.BigDecimal.ZERO;
            roomService.addServiceOption(roomId, nama, satuan, price);
            redirectAttributes.addFlashAttribute("adminSuccess", "Layanan baru ditambahkan.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("adminError", ex.getMessage());
        }
        return "redirect:/admin/layanan";
    }

    @PostMapping("/admin/layanan/{id}/update")
    public String updateServiceOption(
        @PathVariable Long id,
        @RequestParam String nama,
        @RequestParam(required = false) String satuan,
        @RequestParam(required = false) String harga,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }
        try {
            java.math.BigDecimal price = (harga != null && !harga.isBlank()) ? new java.math.BigDecimal(harga.trim()) : java.math.BigDecimal.ZERO;
            roomService.updateServiceOption(id, nama, satuan, price);
            redirectAttributes.addFlashAttribute("adminSuccess", "Layanan diperbarui.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("adminError", ex.getMessage());
        }
        return "redirect:/admin/layanan";
    }

    @PostMapping("/admin/layanan/{id}/delete")
    public String deleteServiceOption(
        @PathVariable Long id,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }
        try {
            roomService.deleteServiceOption(id);
            redirectAttributes.addFlashAttribute("adminSuccess", "Layanan dihapus.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("adminError", ex.getMessage());
        }
        return "redirect:/admin/layanan";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("loginMessage", "Anda telah logout.");
        redirectAttributes.addFlashAttribute("activePanel", "login");
        return "redirect:/login";
    }

    private UserRole resolveUserRole(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return null;
        }
        Object cachedRole = session.getAttribute("userRole");
        if (cachedRole instanceof UserRole role) {
            return role;
        }
        if (cachedRole instanceof String roleName) {
            try {
                UserRole parsed = UserRole.valueOf(roleName);
                session.setAttribute("userRole", parsed);
                return parsed;
            } catch (IllegalArgumentException ignored) {
                // fall through to DB lookup
            }
        }
        return userService
            .findById(userId)
            .map(userService::ensureRole)
            .map(user -> {
                session.setAttribute("userName", user.getFullName());
                session.setAttribute("userEmail", user.getEmail());
                UserRole role = user.getRole() != null ? user.getRole() : UserRole.TAMU;
                session.setAttribute("userRole", role);
                if (role == UserRole.KARYAWAN) {
                    session.setAttribute("userRoles", userService.getEmployeeRoleCodes(user.getId()));
                } else {
                    session.setAttribute("userRoles", java.util.Collections.emptyList());
                }
                return role;
            })
            .orElse(null);
    }

    private String renderDashboardForRole(
        HttpSession session,
        RedirectAttributes redirectAttributes,
        Model model,
        UserRole requiredRole,
        String viewName
    ) {
        UserRole role = resolveUserRole(session);
        if (role == null) {
            session.invalidate();
            redirectAttributes.addFlashAttribute("loginError", "Silakan login terlebih dahulu.");
            return "redirect:/login";
        }
        if (role != requiredRole) {
            return redirectForRole(role);
        }
        populateCommonModel(session, model);
        switch (requiredRole) {
            case ADMIN -> populateAdminModel(model);
            case KARYAWAN -> populateEmployeeModel(model);
            case TAMU -> populateGuestModel(session, model);
        }
        return viewName;
    }

    private String guardRole(HttpSession session, RedirectAttributes redirectAttributes, UserRole requiredRole) {
        UserRole role = resolveUserRole(session);
        if (role == null) {
            session.invalidate();
            redirectAttributes.addFlashAttribute("loginError", "Silakan login terlebih dahulu.");
            return "redirect:/login";
        }
        if (role != requiredRole) {
            return redirectForRole(role);
        }
        return null;
    }

    private String redirectForRole(UserRole role) {
        if (role == null) {
            return "redirect:/dashboard/tamu";
        }
        return switch (role) {
            case ADMIN -> "redirect:/dashboard/admin";
            case KARYAWAN -> "redirect:/dashboard/karyawan";
            case TAMU -> "redirect:/dashboard/tamu";
        };
    }

    private Long requireUserId(HttpSession session, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            session.invalidate();
            redirectAttributes.addFlashAttribute("loginError", "Sesi Anda berakhir. Silakan login ulang.");
        }
        return userId;
    }

    private User requireUser(HttpSession session, RedirectAttributes redirectAttributes) {
        Long userId = requireUserId(session, redirectAttributes);
        if (userId == null) {
            return null;
        }
        return userService
            .findById(userId)
            .orElseGet(() -> {
                session.invalidate();
                redirectAttributes.addFlashAttribute("loginError", "Akun tidak ditemukan. Silakan login ulang.");
                return null;
            });
    }

    private void populateCommonModel(HttpSession session, Model model) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
    }

    private boolean acceptsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private void populateGuestModel(HttpSession session, Model model) {
        Map<String, Object> guest = new HashMap<>();
        guest.put("nama", session.getAttribute("userName"));
        model.addAttribute("guest", guest);
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            reservationService.findActiveForUser(userId)
                .ifPresentOrElse(
                    r -> {
                        model.addAttribute("aktif", reservationService.toActiveView(r));
                        model.addAttribute("layananList", reservationService.serviceOptionsView(r));
                        model.addAttribute("keranjangLayanan", reservationService.serviceCartView(r));
                    },
                    () -> {
                        model.addAttribute("aktif", null);
                        model.addAttribute("layananList", Collections.emptyList());
                        model.addAttribute("keranjangLayanan", Collections.emptyList());
                    }
                );
        } else {
            model.addAttribute("aktif", null);
            model.addAttribute("layananList", Collections.emptyList());
            model.addAttribute("keranjangLayanan", Collections.emptyList());
        }
        model.addAttribute("invoice", userId != null ? paymentService.getLatestPaymentViewForUser(userId) : null);
        model.addAttribute("facilityOptions", roomService.getFacilityOptions());
        if (!model.containsAttribute("layananForm")) {
            model.addAttribute("layananForm", new LayananForm());
        }
    }

    private void populateAdminModel(Model model) {
        List<Room> rooms = roomService.findAllSorted();

        long total = rooms.size();
        long maintenance = rooms.stream().filter(r -> r.getStatus() == RoomStatus.MAINTENANCE).count();

        java.util.Set<Long> occupiedIds = reservationService.getOccupiedRoomIdsToday();
        long occupied = occupiedIds.size();
        long available = Math.max(0, total - occupied - maintenance);

        Map<String, Object> kpi = new HashMap<>();
        kpi.put("availableRooms", available);
        kpi.put("occupiedRooms", occupied);
        kpi.put("todayRevenue", paymentService.getTodayRevenue());
        kpi.put("pendingInvoices", paymentService.getPendingPaymentCount());
        model.addAttribute("kpi", kpi);

        // Build roomByType summary expected by template
        List<Map<String, Object>> byType = RoomType.defaultOrder().stream().map(type -> {
            Map<String, Object> m = new HashMap<>();
            m.put("namaTipe", type.getDisplayName());
            long totalType = rooms.stream().filter(r -> r.getType() == type).count();
            long perawatan = rooms.stream().filter(r -> r.getType() == type && r.getStatus() == RoomStatus.MAINTENANCE).count();
            long terisi = rooms.stream().filter(r -> r.getType() == type && occupiedIds.contains(r.getId())).count();
            long tersedia = Math.max(0, totalType - terisi - perawatan);
            m.put("total", totalType);
            m.put("terisi", terisi);
            m.put("tersedia", tersedia);
            m.put("perawatan", perawatan);
            return m;
        }).toList();
        model.addAttribute("roomByType", byType);

        model.addAttribute("recentReservations", reservationService.recentReservationsView());
        model.addAttribute("recentPayments", paymentService.getRecentPaymentViews(6));
        model.addAttribute("facilityOptions", roomService.getFacilityOptions());
    }

    @GetMapping("/admin/fasilitas")
    public String manageFacilities(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }
        populateCommonModel(session, model);
        model.addAttribute("facilities", roomService.listFacilities());
        return "admin_fasilitas";
    }

    @PostMapping("/admin/fasilitas")
    public String addFacility(
        @RequestParam("name") String name,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }
        try {
            roomService.addFacility(name);
            redirectAttributes.addFlashAttribute("facilitySuccess", "Fasilitas ditambahkan.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("facilityError", ex.getMessage());
        }
        return "redirect:/admin/fasilitas";
    }

    @PostMapping("/admin/fasilitas/{id}/delete")
    public String deleteFacility(
        @PathVariable Long id,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }
        try {
            roomService.deleteFacility(id);
            redirectAttributes.addFlashAttribute("facilitySuccess", "Fasilitas dihapus.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("facilityError", ex.getMessage());
        }
        return "redirect:/admin/fasilitas";
    }

    @GetMapping("/admin/reservasi/{id}")
    public String adminReservationDetail(
        @PathVariable Long id,
        HttpSession session,
        RedirectAttributes redirectAttributes,
        Model model
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.ADMIN);
        if (redirect != null) {
            return redirect;
        }

        return reservationService.findById(id)
            .map(r -> {
                populateCommonModel(session, model);
                model.addAttribute("reservation", r);
                model.addAttribute("payment", paymentService.getLatestPaymentForReservation(id));
                model.addAttribute("payments", paymentService.getPaymentsByReservationId(id));
                model.addAttribute("midtransClientKey", paymentService.getClientKey());
                return "admin_reservasi_detail";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("adminError", "Reservasi tidak ditemukan.");
                return "redirect:/dashboard/admin";
            });
    }

    // ---- Staff (Karyawan): reservation transitions ----
    @PostMapping("/karyawan/reservasi/{id}/checkin")
    public String staffCheckIn(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        String redirect = guardRole(session, redirectAttributes, UserRole.KARYAWAN);
        if (redirect != null) {
            return redirect;
        }
        try {
            reservationService.staffCheckIn(id);
            redirectAttributes.addFlashAttribute("employeeSuccess", "Check-in berhasil.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("employeeError", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("employeeError", "Gagal memproses check-in.");
        }
        return "redirect:/dashboard/karyawan";
    }

    @PostMapping("/karyawan/reservasi/{id}/checkout")
    public String staffCheckOut(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        String redirect = guardRole(session, redirectAttributes, UserRole.KARYAWAN);
        if (redirect != null) {
            return redirect;
        }
        try {
            reservationService.staffCheckOut(id);
            redirectAttributes.addFlashAttribute("employeeSuccess", "Check-out berhasil.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("employeeError", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("employeeError", "Gagal memproses check-out.");
        }
        return "redirect:/dashboard/karyawan";
    }

    @PostMapping("/karyawan/pembayaran/{id}/confirm")
    public String staffConfirmPayment(
        @PathVariable Long id,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.KARYAWAN);
        if (redirect != null) {
            return redirect;
        }
        try {
            paymentService.staffConfirmPayment(id);
            redirectAttributes.addFlashAttribute("employeeSuccess", "Pembayaran dikonfirmasi.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("employeeError", ex.getMessage());
        }
        return "redirect:/dashboard/karyawan";
    }

    @PostMapping("/karyawan/kamar/{id}/available")
    public String markRoomAvailable(
        @PathVariable Long id,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String redirect = guardRole(session, redirectAttributes, UserRole.KARYAWAN);
        if (redirect != null) {
            return redirect;
        }
        try {
            roomService.markAvailableAfterCleaning(id);
            redirectAttributes.addFlashAttribute("employeeSuccess", "Status kamar diperbarui menjadi tersedia.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("employeeError", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("employeeError", "Gagal memperbarui status kamar.");
        }
        return "redirect:/dashboard/karyawan";
    }

    private void populateEmployeeModel(Model model) {
        model.addAttribute("checkinToday", reservationService.arrivalsTodayView());
        model.addAttribute("checkoutToday", reservationService.departuresTodayView());
        model.addAttribute("inhouse", reservationService.inhouseView());
        model.addAttribute("facilityOptions", roomService.getFacilityOptions());
        model.addAttribute("rooms", roomService.findAllSorted());
        model.addAttribute("ordersInProgress", Collections.emptyList());
        model.addAttribute("pendingPayments", paymentService.getPendingPaymentViewsForStaff(10));
    }

    public static class LayananForm {
        private Long layananId;
        private Integer qty = 1;

        public Long getLayananId() {
            return layananId;
        }

        public void setLayananId(Long layananId) {
            this.layananId = layananId;
        }

        public Integer getQty() {
            return qty;
        }

        public void setQty(Integer qty) {
            this.qty = qty;
        }
    }
}
