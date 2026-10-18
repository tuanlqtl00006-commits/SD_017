package com.footstyle.demo.controller;

import com.footstyle.demo.dto.NhanVienRequest;
import com.footstyle.demo.dto.NhanVienResponse;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.service.NhanVienImportService;
import com.footstyle.demo.service.NhanVienService;
import java.io.IOException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/nhan-vien")
@RequiredArgsConstructor
public class NhanVienController {

    private final NhanVienService service;
    private final NhanVienImportService importService;

    @GetMapping
    public List<NhanVienResponse> getAll() {
        return service.getAll();
    }

    /**
     * Nhập HÀNG LOẠT nhân viên từ file Excel (.xlsx). Sai một dòng là từ chối cả file (422 kèm danh sách lỗi theo dòng / cột).
     * chiKiemTra = true: chỉ kiểm tra, chưa lưu. Nhập xong tự gửi mail tài khoản cho từng nhân viên.
     */
    @PostMapping("/import")
    public NhanVienImportService.KetQua importExcel(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "chiKiemTra", required = false, defaultValue = "false") boolean chiKiemTra) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Chưa chọn file Excel để nhập.");
        }
        try {
            return importService.nhap(file.getBytes(), chiKiemTra);
        } catch (IOException e) {
            throw ApiException.badRequest("Không đọc được file đã tải lên.");
        }
    }

    /** Kết quả gửi mail tài khoản của một lần nhập (maLo lấy từ kết quả nhập): tong, daGui, loi, giaLap, xong, lyDo. */
    @GetMapping("/import/ket-qua-mail")
    public Map<String, Object> ketQuaMailImport(@RequestParam("lo") String lo) {
        return importService.ketQuaMail(lo);
    }

    @GetMapping("/{id}")
    public NhanVienResponse getById(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<NhanVienResponse> them(@RequestBody NhanVienRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.them(req));
    }

    @PutMapping("/{id}")
    public NhanVienResponse sua(@PathVariable Integer id, @RequestBody NhanVienRequest req) {
        return service.sua(id, req);
    }

    /** Ẩn / hiện (bật / tắt hoạt động). Không xóa cứng dữ liệu. */
    @PutMapping("/{id}/trang-thai")
    public NhanVienResponse doiTrangThai(@PathVariable Integer id) {
        return service.doiTrangThai(id);
    }

    /** Đổi mật khẩu cho nhân viên (nhân viên tự đổi sau khi đăng nhập). */
    @PutMapping("/{id}/doi-mat-khau")
    public ResponseEntity<Void> doiMatKhau(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        service.doiMatKhau(id, body == null ? "" : body.get("matKhau"));
        return ResponseEntity.noContent().build();
    }
}
