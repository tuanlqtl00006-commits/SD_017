package com.footstyle.demo.controller;

import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.security.XacThuc;
import com.footstyle.demo.service.TroLyNoiBoService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Trợ lý AI nội bộ cho nhân viên / quản lý (nút "Trợ lý AI" trên header trang quản trị). */
@RestController
@RequestMapping("/api/tro-ly")
@RequiredArgsConstructor
public class TroLyNoiBoController {

    static final int DO_DAI_TOI_DA = 500;

    private final TroLyNoiBoService service;

    @GetMapping("/chao")
    public TroLyNoiBoService.TraLoi chao() {
        return service.loiChao(XacThuc.nhanVien());
    }

    @PostMapping("/hoi")
    public TroLyNoiBoService.TraLoi hoi(@RequestBody(required = false) Map<String, String> body) {
        String cauHoi = body == null ? null : body.get("cauHoi");
        if (cauHoi == null || cauHoi.isBlank()) throw ApiException.badRequest("Hãy nhập câu hỏi.");
        if (cauHoi.length() > DO_DAI_TOI_DA) throw ApiException.badRequest("Câu hỏi tối đa " + DO_DAI_TOI_DA + " ký tự.");
        return service.traLoi(cauHoi, XacThuc.nhanVien());
    }
}
