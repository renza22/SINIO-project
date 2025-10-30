package com.sinio.demo.controller;

import com.sinio.demo.dto.LoginRequest;
import com.sinio.demo.dto.RegisterRequest;
import com.sinio.demo.model.User;
import com.sinio.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getFullName());
        session.setAttribute("userEmail", user.getEmail());
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("loginError", "Silakan login terlebih dahulu.");
            return "redirect:/login";
        }
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        return "sinio_dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("loginMessage", "Anda telah logout.");
        redirectAttributes.addFlashAttribute("activePanel", "login");
        return "redirect:/login";
    }
}
