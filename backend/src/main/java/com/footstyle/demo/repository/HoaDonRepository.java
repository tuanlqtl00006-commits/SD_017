package com.footstyle.demo.repository;

import com.footstyle.demo.entity.HoaDon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer>, JpaSpecificationExecutor<HoaDon> {
    Page<HoaDon> findByTrangThai(Integer trangThai, Pageable pageable);
    Page<HoaDon> findByMaHoaDonContainingAndTrangThai(String maHoaDon, Integer trangThai, Pageable pageable);
    Page<HoaDon> findByMaHoaDonContaining(String maHoaDon, Pageable pageable);

    // 1. Sắp xếp từ BÉ đến LỚN (HD001 -> HD012)
    @org.springframework.data.jpa.repository.Query("SELECT h FROM HoaDon h ORDER BY h.id ASC")
    java.util.List<HoaDon> findAllByOrderByIdAsc();

    // 2. Sắp xếp từ LỚN đến BÉ (Mới nhất lên đầu: HD012 -> HD001)
    @org.springframework.data.jpa.repository.Query("SELECT h FROM HoaDon h ORDER BY h.id DESC")
    java.util.List<HoaDon> findAllByOrderByIdDesc();
}