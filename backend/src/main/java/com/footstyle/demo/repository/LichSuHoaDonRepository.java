package com.footstyle.demo.repository;

import com.footstyle.demo.entity.LichSuHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichSuHoaDonRepository extends JpaRepository<LichSuHoaDon, Integer> {
    java.util.List<LichSuHoaDon> findByHoaDon_Id(Integer idHoaDon);
}
