package com.sinio.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // URL http://localhost:8080/login
    @GetMapping("/login")
    public String showLoginPage() {
        return "sinio_login"; // nama file HTML di templates tanpa .html
    }

    // Optional: jika mau akses root langsung ke login
    @GetMapping("/")
    public String home() {
        return "sinio_login";
    }
}
