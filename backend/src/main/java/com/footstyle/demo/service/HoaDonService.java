package com.footstyle.demo.service;

import com.footstyle.demo.dto.HoaDonRequest;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.repository.HoaDonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
}