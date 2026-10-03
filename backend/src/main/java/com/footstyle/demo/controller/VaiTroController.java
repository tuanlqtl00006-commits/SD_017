package com.footstyle.demo.controller;

import com.footstyle.demo.dto.VaiTroResponse;
import com.footstyle.demo.service.NhanVienService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vai-tro")
@RequiredArgsConstructor
public class VaiTroController {

    private final NhanVienService nhanVienService;

    @GetMapping
    public List<VaiTroResponse> getAll() {
        return nhanVienService.getVaiTro();
    }
}
