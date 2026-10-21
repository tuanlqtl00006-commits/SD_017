package com.footstyle.demo.repository;

import com.footstyle.demo.entity.PhieuGiamGia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, Integer> {

    List<PhieuGiamGia> findAllByOrderByIdDesc();

    boolean existsByMaPhieuIgnoreCase(String maPhieu);

    java.util.Optional<PhieuGiamGia> findFirstByMaPhieuIgnoreCase(String maPhieu);

    /**
     * Bán hàng: tăng lượt đã dùng CÓ ĐIỀU KIỆN (chỉ khi phiếu còn lượt). Trả về 0 nếu phiếu vừa hết lượt (2 máy cùng dùng lượt cuối).
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update PhieuGiamGia p set p.soLuongDaDung = coalesce(p.soLuongDaDung, 0) + 1 "
            + "where p.id = :id and (p.soLuong is null or coalesce(p.soLuongDaDung, 0) < p.soLuong)")
    int tangLuotDung(@org.springframework.data.repository.query.Param("id") Integer id);
}
