package com.footstyle.demo.controller;

import com.footstyle.demo.service.ChuyenKhoanQuayService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Trang ngân hàng giả cho chuyển khoản tại quầy (công khai, chỉ khi bật thanh toán giả lập). Xem ChuyenKhoanQuayService. */
@RestController
@RequestMapping("/api/cua-hang/thanh-toan-gia-lap/quay")
@RequiredArgsConstructor
public class ThanhToanGiaLapQuayController {

    private final ChuyenKhoanQuayService service;

    @GetMapping("/{maHoaDon}")
    public Map<String, Object> xem(@PathVariable String maHoaDon) {
        return service.xemChoNganHang(maHoaDon);
    }

    @PostMapping("/{maHoaDon}/xac-nhan")
    public Map<String, Object> xacNhan(@PathVariable String maHoaDon) {
        return service.xacNhanTuNganHang(maHoaDon);
    }
}
