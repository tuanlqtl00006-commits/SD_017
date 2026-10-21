package com.footstyle.demo.repository;

import com.footstyle.demo.entity.PhieuGiamGiaKhachHang;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhieuGiamGiaKhachHangRepository extends JpaRepository<PhieuGiamGiaKhachHang, Integer> {

    List<PhieuGiamGiaKhachHang> findByIdPhieuGiamGia(Integer idPhieuGiamGia);

    /** Phiếu cá nhân được tặng cho một khách (bán hàng: liệt kê phiếu khách dùng được). */
    List<PhieuGiamGiaKhachHang> findByIdKhachHang(Integer idKhachHang);
}
