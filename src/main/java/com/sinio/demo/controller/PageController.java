package com.sinio.demo.controller;

import com.sinio.demo.dto.EmployeeRequest;
import com.sinio.demo.dto.LoginRequest;
import com.sinio.demo.dto.RegisterRequest;
import com.sinio.demo.model.User;
import com.sinio.demo.model.UserRole;
import com.sinio.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PageController {

    private final UserService userService;

    public PageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping({"/", "/login"})
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
        model.addAttribute("employees", userService.findAllEmployees());

        if (!model.containsAttribute("createForm")) {
            model.addAttribute("createForm", new EmployeeRequest());
        }
        if (!model.containsAttribute("editForm")) {
            model.addAttribute("editForm", new EmployeeRequest());
        }
        if (!model.containsAttribute("formMode")) {
            model.addAttribute("formMode", "create");
        }
        return "admin_crud_karyawan";
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

    private void populateCommonModel(HttpSession session, Model model) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
    }

    private void populateGuestModel(HttpSession session, Model model) {
        Map<String, Object> guest = new HashMap<>();
        guest.put("nama", session.getAttribute("userName"));
        model.addAttribute("guest", guest);
        model.addAttribute("aktif", null);
        model.addAttribute("invoice", null);
        model.addAttribute("layananList", Collections.emptyList());
        model.addAttribute("keranjangLayanan", Collections.emptyList());
        model.addAttribute("layananForm", new LayananForm());
    }

    private void populateAdminModel(Model model) {
        Map<String, Object> kpi = new HashMap<>();
        kpi.put("availableRooms", 0);
        kpi.put("occupiedRooms", 0);
        kpi.put("todayRevenue", BigDecimal.ZERO);
        kpi.put("pendingInvoices", 0);
        model.addAttribute("kpi", kpi);
        model.addAttribute("recentReservations", Collections.emptyList());
        model.addAttribute("recentPayments", Collections.emptyList());
        model.addAttribute("roomByType", Collections.emptyList());
    }

    private void populateEmployeeModel(Model model) {
        model.addAttribute("checkinToday", Collections.emptyList());
        model.addAttribute("checkoutToday", Collections.emptyList());
        model.addAttribute("ordersInProgress", Collections.emptyList());
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
