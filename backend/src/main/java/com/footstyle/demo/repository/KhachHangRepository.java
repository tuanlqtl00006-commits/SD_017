package com.footstyle.demo.repository;

import com.footstyle.demo.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT k FROM KhachHang k WHERE " +
           "LOWER(k.ten) LIKE LOWER(CONCAT(:keyword, '%')) OR " +
           "LOWER(k.sdt) LIKE LOWER(CONCAT(:keyword, '%')) OR " +
           "LOWER(k.email) LIKE LOWER(CONCAT(:keyword, '%'))")
        List<KhachHang> searchKhachHang(@org.springframework.data.repository.query.Param("keyword") String keyword);
    boolean existsByEmail(String email);
    boolean existsBySdt(String sdt);

    List<KhachHang> findByTrangThaiOrderByTenAsc(Integer trangThai);

}



