package com.footstyle.demo.controller;

import com.footstyle.demo.dto.KhachHangTomTatResponse;
import com.footstyle.demo.dto.PhieuGiamGiaRequest;
import com.footstyle.demo.dto.PhieuGiamGiaResponse;
import com.footstyle.demo.service.MailService;
import com.footstyle.demo.service.PhieuGiamGiaService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/phieu-giam-gia")
@RequiredArgsConstructor
public class PhieuGiamGiaController {

    private final PhieuGiamGiaService service;
    private final MailService mailService;

    /**
     * Thử cấu hình Gmail: gửi NGAY một thư thử đến chính email gửi (không gửi cho ai khác) và trả về kết quả / lỗi thật.
     * Mở thẳng trên trình duyệt: http://localhost:8081/api/phieu-giam-gia/thu-gui-mail
     */
    @GetMapping("/thu-gui-mail")
    public Map<String, Object> thuGuiMail() {
        String ketQua = mailService.guiThuThu();
        return Map.of("daCauHinh", mailService.duocCauHinh(), "ketQua", ketQua);
    }

    /** Trạng thái gửi mail (đã cấu hình Gmail chưa, đăng nhập thử lúc khởi động thành công hay lỗi + lý do). */
    @GetMapping("/trang-thai-mail")
    public Map<String, Object> trangThaiMail() {
        return mailService.trangThai();
    }

    /** Kết quả gửi mail của lần lưu phiếu gần nhất: { tong, daGui, loi, giaLap, xong, lyDo }. */
    @GetMapping("/{id}/ket-qua-mail")
    public Map<String, Object> ketQuaMail(@PathVariable Integer id) {
        return service.ketQuaMail(id);
    }

    @GetMapping
    public List<PhieuGiamGiaResponse> getAll() {
        return service.getAll();
    }

    /** Khách hàng có thể chọn khi tặng phiếu cá nhân. */
    @GetMapping("/khach-hang")
    public List<KhachHangTomTatResponse> getKhachHangCoTheChon() {
        return service.getKhachHangCoTheChon();
    }

    @GetMapping("/{id}")
    public PhieuGiamGiaResponse getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<PhieuGiamGiaResponse> them(@RequestBody PhieuGiamGiaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.them(req));
    }

    @PutMapping("/{id}")
    public PhieuGiamGiaResponse sua(@PathVariable Integer id, @RequestBody PhieuGiamGiaRequest req) {
        return service.sua(id, req);
    }

    /** Ẩn / hiện (bật / tắt hoạt động). Không xóa cứng dữ liệu. */
    @PutMapping("/{id}/trang-thai")
    public PhieuGiamGiaResponse doiTrangThai(@PathVariable Integer id) {
        return service.doiTrangThai(id);
    }
}
