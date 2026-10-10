package com.footstyle.demo.controller;

import com.footstyle.demo.dto.ViTriRequest;
import com.footstyle.demo.dto.ViTriResponse;
import com.footstyle.demo.service.NhanVienService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Vị trí làm việc của nhân viên. Chỉ có xem danh sách và thêm mới, KHÔNG có xóa. */
@RestController
@RequestMapping("/api/vi-tri")
@RequiredArgsConstructor
public class ViTriController {

    private final NhanVienService nhanVienService;

    @GetMapping
    public List<ViTriResponse> getAll() {
        return nhanVienService.getViTri();
    }

    @PostMapping
    public ResponseEntity<ViTriResponse> them(@RequestBody ViTriRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nhanVienService.themViTri(req));
    }
}
