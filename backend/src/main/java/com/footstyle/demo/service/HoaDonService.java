package com.footstyle.demo.service;

import com.footstyle.demo.dto.HoaDonRequest;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.repository.HoaDonChiTietRepository;
import com.footstyle.demo.repository.HoaDonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
public class HoaDonService {

    @Autowired
    private HoaDonRepository hoaDonRepository;
    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    public Page<HoaDonRequest> getDanhSach(Integer trangThai, Pageable pageable) {
        Page<HoaDon> pageHoaDon;

        if (trangThai == null) {
            pageHoaDon = hoaDonRepository.findAll(pageable);
        } else {
            pageHoaDon = hoaDonRepository.findByTrangThai(trangThai, pageable);
        }

        return pageHoaDon.map(hd -> {
            HoaDonRequest request = new HoaDonRequest();
            request.setId(hd.getId() != null ? hd.getId().intValue() : null);
            request.setMaHoaDon(hd.getMaHoaDon());

            request.setTenKhachHang(hd.getKhachHang() != null ? hd.getKhachHang().getTen() : "Khách lẻ");
            request.setTenNhanVien(hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "Không xác định");

            request.setTongTien(hd.getTongTien());
            request.setNgayTao(hd.getNgayTao());
            request.setTrangThai(hd.getTrangThai());

            
            if (hd.getLoaiDon() != null) {
                if (hd.getLoaiDon() == 1) {
                    request.setLoaiDon("Tại quầy");
                } else if (hd.getLoaiDon() == 2) {
                    request.setLoaiDon("Online");
                } else {
                    request.setLoaiDon("Khác");
                }
            } else {
                
                request.setLoaiDon("Tại quầy");
            }
            

            return request;
        });
    }

    public Page<HoaDonRequest> getDanhSachCoLoc(String maHoaDon, java.time.LocalDate tuNgay, java.time.LocalDate denNgay, Integer loaiDon, Integer trangThai, Pageable pageable) {
        
        Specification<HoaDon> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            
            if (maHoaDon != null && !maHoaDon.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("maHoaDon"), "%" + maHoaDon.trim() + "%"));
            }

            
            if (tuNgay != null) {
                java.time.LocalDateTime startOfDay = tuNgay.atStartOfDay();
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("ngayTao"), startOfDay));
            }

            
            if (denNgay != null) {
                java.time.LocalDateTime endOfDay = denNgay.atTime(23, 59, 59);
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("ngayTao"), endOfDay));
            }

            
            if (loaiDon != null) {
                
                predicates.add(criteriaBuilder.equal(root.get("loaiDon"), loaiDon));
            }

            
            if (trangThai != null) {
                predicates.add(criteriaBuilder.equal(root.get("trangThai"), trangThai));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<HoaDon> pageHoaDon = hoaDonRepository.findAll(spec, pageable);

        
        return pageHoaDon.map(hd -> {
            HoaDonRequest request = new HoaDonRequest();
            request.setId(hd.getId() != null ? hd.getId().intValue() : null);
            request.setMaHoaDon(hd.getMaHoaDon());
            request.setTenKhachHang(hd.getKhachHang() != null ? hd.getKhachHang().getTen() : "Khách lẻ");
            request.setTenNhanVien(hd.getNhanVien() != null ? hd.getNhanVien().getHoTen() : "Không xác định");
            request.setTongTien(hd.getTongTien());
            request.setNgayTao(hd.getNgayTao());
            request.setTrangThai(hd.getTrangThai());

            if (hd.getLoaiDon() != null) {
                if (hd.getLoaiDon() == 1) {
                    request.setLoaiDon("Tại quầy");
                } else if (hd.getLoaiDon() == 2) {
                    request.setLoaiDon("Online");
                } else {
                    request.setLoaiDon("Khác");
                }
            } else {
                request.setLoaiDon("Tại quầy");
            }
            return request;
        });
    }

    public java.util.Map<String, Object> getChiTietHoaDon(Integer idHoaDon) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();

        // 1. Lấy thông tin chung của Hóa Đơn
        HoaDon hd = hoaDonRepository.findById(idHoaDon).orElse(null);
        if (hd == null) return response;

        java.util.Map<String, Object> hoaDonInfo = new java.util.HashMap<>();
        hoaDonInfo.put("maHoaDon", hd.getMaHoaDon());
        hoaDonInfo.put("tenKhachHang", hd.getKhachHang() != null ? hd.getKhachHang().getTen() : "Khách vãng lai");
        hoaDonInfo.put("sdtNguoiNhan", hd.getKhachHang() != null ? hd.getKhachHang().getSdt() : null);
        hoaDonInfo.put("ngayTao", hd.getNgayTao());
        hoaDonInfo.put("trangThai", hd.getTrangThai());
        hoaDonInfo.put("tongTien", hd.getTongTien());
        
        response.put("hoaDon", hoaDonInfo);

        // 2. Lấy danh sách sản phẩm nằm trong hóa đơn đó
        List<com.footstyle.demo.entity.HoaDonChiTiet> listHDCT = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
        List<java.util.Map<String, Object>> chiTietList = new ArrayList<>();
        
        for (com.footstyle.demo.entity.HoaDonChiTiet ct : listHDCT) {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            
            // LƯU Ý: Chỗ này tùy thuộc vào Entity HoaDonChiTiet của bạn liên kết với SanPham như thế nào.
            // Nếu bạn đang nối với SanPhamChiTiet, hãy sửa thành: ct.getSanPhamChiTiet().getSanPham().getTenSanPham()
            // Tạm thời mình để chữ "Sản phẩm Demo" để bạn test code chạy mượt trước nhé.
            item.put("tenSanPham", "Sản phẩm Demo"); 
            item.put("soLuong", ct.getSoLuong());
            item.put("donGia", ct.getDonGia());
            
            chiTietList.add(item);
        }
        
        response.put("chiTietList", chiTietList);
        return response;
    }
}