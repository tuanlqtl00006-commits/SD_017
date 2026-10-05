package com.footstyle.demo.repository;

import com.footstyle.demo.entity.HoaDonChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Integer> {

    // Tìm danh sách chi tiết của 1 hóa đơn
    List<HoaDonChiTiet> findByHoaDon_Id(Integer idHoaDon);

}