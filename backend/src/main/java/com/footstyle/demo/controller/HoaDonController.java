package com.footstyle.demo.controller;

import com.footstyle.demo.dto.HoaDonRequest;
import com.footstyle.demo.service.HoaDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.LichSuHoaDon;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.repository.LichSuHoaDonRepository;

@RestController
@RequestMapping("/api/hoa-don")
public class HoaDonController {

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private LichSuHoaDonRepository lichSuHoaDonRepository;

    @PutMapping("/cap-nhat-trang-thai/{id}")
    public ResponseEntity<?> capNhatTrangThai(
            @PathVariable("id") Integer id, 
            @RequestParam("trangThaiMoi") Integer trangThaiMoi,
            @RequestParam(value = "ghiChu", required = false) String ghiChu) {
        
        try {
            // 1. Tìm hóa đơn
            HoaDon hoaDon = hoaDonRepository.findById(id).orElse(null);
            if (hoaDon == null) {
                return ResponseEntity.badRequest().body("Không tìm thấy hóa đơn!");
            }
            
            // 2. Cập nhật trạng thái hóa đơn mới
            hoaDon.setTrangThai(trangThaiMoi);
            hoaDonRepository.save(hoaDon);
            
            // 3. TẠO VÀ LƯU LỊCH SỬ THAO TÁC
            LichSuHoaDon lichSu = new LichSuHoaDon();
            lichSu.setHoaDon(hoaDon);
            lichSu.setTrangThai(trangThaiMoi);
            
            // ĐỔI TÊN Ở ĐÂY: (Sau này có form Đăng nhập thì lấy tên từ tài khoản ra)
            lichSu.setNguoiTao("ADMIN - Nguyễn Trịnh Phương Minh"); 

            // Cập nhật tên hành động cho đúng với trạng thái thay vì câu mặc định
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
            lichSu.setGhiChu(ghiChu != null && !ghiChu.isEmpty() ? ghiChu : tenTrangThai);
            
            lichSu.setNgayTao(new Date()); 
            
            lichSuHoaDonRepository.save(lichSu);
            
            return ResponseEntity.ok("Cập nhật trạng thái và lưu lịch sử thành công!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getListHoaDon(
            @RequestParam(required = false) String maHoaDon,
            
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            
            @RequestParam(required = false) Integer loaiDon,
            
            @RequestParam(required = false) Integer trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        
        
        Page<HoaDonRequest> responsePage = hoaDonService.getDanhSachCoLoc(maHoaDon, tuNgay, denNgay, loaiDon, trangThai, pageable);
        
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("content", responsePage.getContent());     
        responseMap.put("totalPages", responsePage.getTotalPages()); 
        responseMap.put("number", responsePage.getNumber());         
        
        return ResponseEntity.ok(responseMap); 
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getChiTietHoaDon(@PathVariable Integer id) {
        Map<String, Object> result = hoaDonService.getChiTietHoaDon(id);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // TÌM THÊM DANH SÁCH LỊCH SỬ CỦA HÓA ĐƠN NÀY
        java.util.List<LichSuHoaDon> lichSuList = lichSuHoaDonRepository.findByHoaDon_Id(id);
        
        // THÊM DÒNG NÀY ĐỂ TRẢ VỀ CÙNG VỚI HÓA ĐƠN VÀ CHI TIẾT
        result.put("danhSachLichSu", lichSuList);

        return ResponseEntity.ok(result);
    }
}