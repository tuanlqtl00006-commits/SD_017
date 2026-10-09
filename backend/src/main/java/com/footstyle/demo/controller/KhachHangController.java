package com.footstyle.demo.controller;

import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.repository.KhachHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/khach-hang")
public class KhachHangController {

    @Autowired
    private KhachHangRepository khachHangRepository;

    // Giới hạn tuổi khách hàng (frontend: src/utils/ngaySinhKhachHang.js cũng dùng 14 và 120)
    private static final int TUOI_TOI_THIEU = 14;
    private static final int TUOI_TOI_DA = 120;

    /**
     * Kiểm tra ngày sinh: bắt buộc, không ở tương lai, từ 14 đến 120 tuổi.
     * Ngày không có thật (30/02...) hoặc sai định dạng yyyy-MM-dd đã bị Jackson chặn từ lúc đọc JSON (trả 400).
     * Trả về câu báo lỗi, hoặc null nếu hợp lệ.
     */
    private String kiemTraNgaySinh(LocalDate ngaySinh) {
        if (ngaySinh == null) {
            return "Vui lòng chọn ngày sinh.";
        }
        LocalDate homNay = LocalDate.now();
        if (ngaySinh.isAfter(homNay)) {
            return "Ngày sinh không được lớn hơn ngày hiện tại.";
        }
        if (ngaySinh.isAfter(homNay.minusYears(TUOI_TOI_THIEU))) {
            return "Khách hàng phải từ " + TUOI_TOI_THIEU + " tuổi trở lên.";
        }
        LocalDate ngaySinhSomNhat = homNay.minusYears(TUOI_TOI_DA);
        if (ngaySinh.isBefore(ngaySinhSomNhat)) {
            return "Tuổi tối đa là " + TUOI_TOI_DA + " (ngày sinh không được trước "
                    + ngaySinhSomNhat.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ").";
        }
        return null;
    }

    @GetMapping
    public List<KhachHang> getAllKhachHang(@RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return khachHangRepository.searchKhachHang(search);
        }
        return khachHangRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KhachHang> getKhachHangById(@PathVariable Integer id) {
        Optional<KhachHang> khachHang = khachHangRepository.findById(id);
        return khachHang.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createKhachHang(@RequestBody KhachHang khachHang) {
        if (khachHang.getEmail() != null && !khachHang.getEmail().isEmpty() && khachHangRepository.existsByEmail(khachHang.getEmail())) {
            return ResponseEntity.status(409).body(java.util.Collections.singletonMap("message", "Email đã tồn tại."));
        }
        if (khachHang.getSdt() != null && !khachHang.getSdt().isEmpty() && khachHangRepository.existsBySdt(khachHang.getSdt())) {
            return ResponseEntity.status(409).body(java.util.Collections.singletonMap("message", "Số điện thoại đã tồn tại."));
        }

        if (khachHang.getHoTen() == null || khachHang.getHoTen().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", "Vui lòng nhập họ tên khách hàng."));
        }

        String loiNgaySinh = kiemTraNgaySinh(khachHang.getNgaySinh());
        if (loiNgaySinh != null) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", loiNgaySinh));
        }

        if (khachHang.getMaKhachHang() == null || khachHang.getMaKhachHang().isEmpty()) {
            khachHang.setMaKhachHang("KH" + System.currentTimeMillis());
        }
        
        // Auto-generate password from phone number
        if (khachHang.getSdt() != null) {
            khachHang.setMatKhau(java.util.UUID.randomUUID().toString().substring(0, 8));
        }

        if (khachHang.getTrangThai() == null) {
            khachHang.setTrangThai(1);
        }

        // Địa chỉ gửi kèm: trường khachHang bị @JsonIgnore nên phải tự gắn lại, nếu không id_khach_hang sẽ NULL
        if (khachHang.getDiaChiList() != null) {
            khachHang.getDiaChiList().forEach(dc -> {
                dc.setKhachHang(khachHang);
                if (dc.getTrangThai() == null) dc.setTrangThai(1);
            });
        }
        
        try {
            return ResponseEntity.ok(khachHangRepository.save(khachHang));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
            return ResponseEntity.status(409).body(java.util.Collections.singletonMap("message", "Lỗi lưu dữ liệu: " + rootMsg));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateKhachHang(@PathVariable Integer id, @RequestBody KhachHang khachHangDetails) {
        Optional<KhachHang> optionalKhachHang = khachHangRepository.findById(id);
        if (optionalKhachHang.isPresent()) {
            KhachHang khachHang = optionalKhachHang.get();

            // Check if changing email to an existing one
            if (khachHangDetails.getEmail() != null && !khachHangDetails.getEmail().equals(khachHang.getEmail()) && khachHangRepository.existsByEmail(khachHangDetails.getEmail())) {
                return ResponseEntity.status(409).body(java.util.Collections.singletonMap("message", "Email đã tồn tại."));
            }
            // Check if changing sdt to an existing one
            if (khachHangDetails.getSdt() != null && !khachHangDetails.getSdt().equals(khachHang.getSdt()) && khachHangRepository.existsBySdt(khachHangDetails.getSdt())) {
                return ResponseEntity.status(409).body(java.util.Collections.singletonMap("message", "Số điện thoại đã tồn tại."));
            }

            if (khachHangDetails.getHoTen() != null && !khachHangDetails.getHoTen().trim().isEmpty()) {
                khachHang.setHoTen(khachHangDetails.getHoTen().trim());
            }

            // Chỉ kiểm tra khi ngày sinh thực sự bị đổi (nút bật/tắt trạng thái gửi lại ngày sinh cũ,
            // khách cũ chưa có ngày sinh vẫn đổi trạng thái được)
            if (!Objects.equals(khachHangDetails.getNgaySinh(), khachHang.getNgaySinh())) {
                String loiNgaySinh = kiemTraNgaySinh(khachHangDetails.getNgaySinh());
                if (loiNgaySinh != null) {
                    return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", loiNgaySinh));
                }
            }
            khachHang.setSdt(khachHangDetails.getSdt());
            khachHang.setEmail(khachHangDetails.getEmail());
            khachHang.setNgaySinh(khachHangDetails.getNgaySinh());
            khachHang.setGioiTinh(khachHangDetails.getGioiTinh());
            if (khachHangDetails.getAnhDaiDien() != null) {
                khachHang.setAnhDaiDien(khachHangDetails.getAnhDaiDien().isBlank() ? null : khachHangDetails.getAnhDaiDien());
            }
            if (khachHangDetails.getTrangThai() != null) {
                khachHang.setTrangThai(khachHangDetails.getTrangThai());
            }
            
            return ResponseEntity.ok(khachHangRepository.save(khachHang));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKhachHang(@PathVariable Integer id) {
        Optional<KhachHang> khachHang = khachHangRepository.findById(id);
        if (khachHang.isPresent()) {
            khachHangRepository.delete(khachHang.get());
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}




