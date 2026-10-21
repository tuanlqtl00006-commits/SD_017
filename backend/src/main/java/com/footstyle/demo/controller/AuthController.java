package com.footstyle.demo.controller;

import com.footstyle.demo.dto.DangNhapRequest;
import com.footstyle.demo.dto.DoiMatKhauRequest;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.XacThuc;
import com.footstyle.demo.security.XacThucInterceptor;
import com.footstyle.demo.service.AuthService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Đăng nhập / tài khoản. Xem AuthService và security/PhanQuyen. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;
    private final XacThucInterceptor interceptor;

    /** Công khai: giao diện hỏi có bật đăng nhập không (app.auth.bat). */
    @GetMapping("/cau-hinh")
    public Map<String, Object> cauHinh() {
        return Map.of("batDangNhap", interceptor.batDangNhap());
    }

    @PostMapping("/dang-nhap")
    public Map<String, Object> dangNhap(@RequestBody(required = false) DangNhapRequest req) {
        return service.dangNhapNhanVien(req);
    }

    @PostMapping("/khach-hang/dang-nhap")
    public Map<String, Object> dangNhapKhachHang(@RequestBody(required = false) DangNhapRequest req) {
        return service.dangNhapKhachHang(req);
    }

    /** Người đang đăng nhập. */
    @GetMapping("/toi")
    public Map<String, Object> toi() {
        NguoiDung nd = XacThuc.hienTai()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập."));
        Map<String, Object> m = new LinkedHashMap<>(AuthService.thongTin(nd));
        m.put("batDangNhap", interceptor.batDangNhap());
        return m;
    }

    @PutMapping("/doi-mat-khau")
    public Map<String, Object> doiMatKhau(@RequestBody(required = false) DoiMatKhauRequest req) {
        return service.doiMatKhau(req);
    }

    @PostMapping("/quen-mat-khau")
    public Map<String, Object> quenMatKhau(@RequestBody(required = false) Map<String, String> body) {
        return service.quenMatKhau(body == null ? null : body.get("email"));
    }
}
