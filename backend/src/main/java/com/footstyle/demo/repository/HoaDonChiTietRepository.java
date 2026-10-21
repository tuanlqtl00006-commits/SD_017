package com.footstyle.demo.repository;

import com.footstyle.demo.entity.HoaDonChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Integer> {
    List<HoaDonChiTiet> findByHoaDon_IdOrderByIdAsc(Integer idHoaDon);

    /** Chi tiết của các hóa đơn có trạng thái `trangThai`, tạo trong khoảng [tu, den] (thống kê sản phẩm bán chạy). */
    List<HoaDonChiTiet> findByHoaDon_TrangThaiAndHoaDon_NgayTaoBetween(Integer trangThai, java.time.LocalDateTime tu, java.time.LocalDateTime den);
}
