package com.footstyle.demo.controller;

import com.footstyle.demo.service.CaiDatVanChuyenService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cài đặt cửa hàng, chỉ Quản lý (PhanQuyen: đường dẫn không thuộc nhóm nhân viên nên nhân viên bị chặn).
 * Giao diện bán hàng đọc cài đặt qua GET /api/cua-hang/cai-dat-van-chuyen (công khai).
 */
@RestController
@RequestMapping("/api/cai-dat")
@RequiredArgsConstructor
public class CaiDatController {

    private final CaiDatVanChuyenService vanChuyen;

    /** mienPhiBat: bật miễn phí theo giá trị đơn; mienPhiTu: tiền hàng từ bao nhiêu thì miễn phí. */
    public record VanChuyenRequest(Boolean mienPhiBat, Long mienPhiTu) {
    }

    @GetMapping("/van-chuyen")
    public Map<String, Object> xemVanChuyen() {
        return vanChuyen.moTaQuanLy();
    }

    @PutMapping("/van-chuyen")
    public Map<String, Object> luuVanChuyen(@RequestBody(required = false) VanChuyenRequest req) {
        vanChuyen.luu(req == null ? null : req.mienPhiBat(), req == null ? null : req.mienPhiTu());
        return vanChuyen.moTaQuanLy();
    }
}
