package com.footstyle.demo.repository;

import com.footstyle.demo.entity.LichSuHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuHoaDonRepository extends JpaRepository<LichSuHoaDon, Integer> {
    List<LichSuHoaDon> findByHoaDon_Id(Integer idHoaDon);

    /** Lịch sử của một hóa đơn, cũ trước mới sau (đúng thứ tự trên dòng thời gian). */
    List<LichSuHoaDon> findByHoaDon_IdOrderByThoiGianAscIdAsc(Integer idHoaDon);
}
