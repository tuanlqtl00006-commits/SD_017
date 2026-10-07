package com.footstyle.demo.controller;

import com.footstyle.demo.dto.HoaDonRequest;
import com.footstyle.demo.dto.HoaDonTrangThaiRequest;
import com.footstyle.demo.service.HoaDonService;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.LichSuHoaDonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/hoa-don")
@RequiredArgsConstructor
public class HoaDonController {

    private final HoaDonService hoaDonService;
    private final HoaDonRepository hoaDonRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;

    @PutMapping("/cap-nhat-trang-thai/{id}")
    public ResponseEntity<?> capNhatTrangThai(
            @PathVariable("id") Integer id, 
            @RequestParam("trangThaiMoi") Integer trangThaiMoi,
            @RequestParam(value = "ghiChu", required = false) String ghiChu) {
        
        try {
            HoaDon hoaDon = hoaDonRepository.findById(id).orElse(null);
            if (hoaDon == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy hóa đơn!");
            }
            
            hoaDon.setTrangThai(trangThaiMoi);
            hoaDonRepository.save(hoaDon);
            
            LichSuHoaDon lichSu = new LichSuHoaDon();
            lichSu.setHoaDon(hoaDon);
            lichSu.setTrangThai(trangThaiMoi);
            lichSu.setNguoiThaoTac("ADMIN - Nguyễn Trịnh Phương Minh"); 

            String tenTrangThai = "";
            switch (trangThaiMoi) {
                case 0: tenTrangThai = "Hủy đơn hàng"; break;
                case 1: tenTrangThai = "Chờ xác nhận"; break;
                case 2: tenTrangThai = "Đã xác nhận"; break;
                case 3: tenTrangThai = "Chờ lấy hàng"; break;
                case 4: tenTrangThai = "Đang giao hàng"; break;
                case 5: tenTrangThai = "Đã giao hàng"; break;
                case 6: tenTrangThai = "Hoàn thành"; break;
                default: tenTrangThai = "Cập nhật trạng thái";
            }
            lichSu.setMoTa(ghiChu != null && !ghiChu.isEmpty() ? ghiChu : tenTrangThai);
            lichSu.setHanhDong(tenTrangThai);
            lichSu.setThoiGian(LocalDateTime.now());
            lichSu.setNgayTao(new Date()); 
            
            lichSuHoaDonRepository.save(lichSu);
            
            return ResponseEntity.ok("Cập nhật trạng thái và lưu lịch sử thành công!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping
    public Map<String, Object> getListHoaDon(
            @RequestParam(required = false) String maHoaDon,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) Integer loaiDon,
            @RequestParam(required = false) Integer trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").descending());
        Page<HoaDonRequest> responsePage = hoaDonService.getDanhSachCoLoc(maHoaDon, tuNgay, denNgay, loaiDon, trangThai, pageable);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", responsePage.getContent());
        responseMap.put("totalPages", responsePage.getTotalPages());
        responseMap.put("number", responsePage.getNumber());
        responseMap.put("totalElements", responsePage.getTotalElements());
        return responseMap;
    }

    /** Toàn bộ dữ liệu cho trang chi tiết: thông tin, khách, giao hàng, sản phẩm, thanh toán, lịch sử. */
    @GetMapping("/{id}")
    public Map<String, Object> getChiTietHoaDon(@PathVariable Integer id) {
        Map<String, Object> result = hoaDonService.getChiTietHoaDon(id);
        if (result != null && !result.isEmpty() && !result.containsKey("danhSachLichSu")) {
            java.util.List<LichSuHoaDon> lichSuList = lichSuHoaDonRepository.findByHoaDon_Id(id);
            result.put("danhSachLichSu", lichSuList);
        }
        return result;
    }

    /** Chuyển sang trạng thái tiếp theo hoặc hủy đơn; trả về chi tiết hóa đơn sau khi đổi. */
    @PutMapping("/{id}/trang-thai")
    public Map<String, Object> doiTrangThai(@PathVariable Integer id, @RequestBody HoaDonTrangThaiRequest req) {
        return hoaDonService.doiTrangThai(id, req);
    }
}
