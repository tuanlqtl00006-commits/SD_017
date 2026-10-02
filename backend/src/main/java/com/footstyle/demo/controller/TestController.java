package com.footstyle.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/test-connection")
    public String testConnection() {
        return "Kết nối giữa Frontend (Vue 3) và Backend (Spring Boot) thành công!";
    }
}
