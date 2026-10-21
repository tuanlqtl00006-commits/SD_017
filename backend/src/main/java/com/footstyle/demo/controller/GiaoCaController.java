package com.footstyle.demo.controller;

import com.footstyle.demo.service.GiaoCaService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Nhận ca / giao ca của nhân viên đang đăng nhập + lịch sử giao ca. */
@RestController
@RequestMapping("/api/giao-ca")
@RequiredArgsConstructor
public class GiaoCaController {

    private final GiaoCaService service;

    @GetMapping("/hien-tai")
    public Map<String, Object> hienTai() {
        return service.hienTai();
    }

    @PostMapping("/nhan-ca")
    public Map<String, Object> nhanCa(@RequestBody(required = false) GiaoCaService.NhanCaRequest req) {
        return service.nhanCa(req);
    }

    @PostMapping("/ket-ca")
    public Map<String, Object> ketCa(@RequestBody(required = false) GiaoCaService.KetCaRequest req) {
        return service.ketCa(req);
    }

    @GetMapping("/nhan-vien")
    public List<Map<String, Object>> nhanVienNhanBanGiao() {
        return service.nhanVienNhanBanGiao();
    }

    @GetMapping
    public List<Map<String, Object>> danhSach(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Integer idNhanVien) {
        return service.danhSach(tuNgay, denNgay, idNhanVien);
    }

    @GetMapping("/{id}")
    public Map<String, Object> chiTiet(@PathVariable Integer id) {
        return service.chiTiet(id);
    }
}
