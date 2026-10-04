package com.footstyle.demo.service;

import com.footstyle.demo.dto.HoaDonRequest;
import com.footstyle.demo.entity.HoaDon;
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
}