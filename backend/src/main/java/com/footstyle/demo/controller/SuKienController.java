package com.footstyle.demo.controller;

import com.footstyle.demo.service.SuKienService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Kênh báo thay đổi dữ liệu cho giao diện (công khai: tin chỉ có tên chủ đề, không có dữ liệu). Xem SuKienService. */
@RestController
@RequestMapping("/api/su-kien")
@RequiredArgsConstructor
public class SuKienController {

    private final SuKienService suKien;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter nghe(HttpServletResponse res) {
        res.setHeader("Cache-Control", "no-cache");
        res.setHeader("X-Accel-Buffering", "no"); // proxy (nginx) không gom tin lại
        return suKien.moKetNoi();
    }
}
