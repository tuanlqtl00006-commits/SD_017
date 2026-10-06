package com.footstyle.demo.repository;

import com.footstyle.demo.entity.KhachHang;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {

    List<KhachHang> findByTrangThaiOrderByHoTenAsc(Integer trangThai);
    
    boolean existsByEmail(String email);
    
    boolean existsBySdt(String sdt);
    
    @Query("SELECT k FROM KhachHang k WHERE k.hoTen LIKE %:keyword% OR k.sdt LIKE %:keyword% OR k.email LIKE %:keyword%")
    List<KhachHang> searchKhachHang(@Param("keyword") String keyword);
}
