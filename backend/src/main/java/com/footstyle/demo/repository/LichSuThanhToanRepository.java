package com.footstyle.demo.repository;

import com.footstyle.demo.entity.LichSuThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuThanhToanRepository extends JpaRepository<LichSuThanhToan, Integer> {
    List<LichSuThanhToan> findByHoaDon_IdOrderByThoiGianAscIdAsc(Integer idHoaDon);
}
