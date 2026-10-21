package com.footstyle.demo.repository;

import com.footstyle.demo.entity.LichSuThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuThanhToanRepository extends JpaRepository<LichSuThanhToan, Integer> {
    List<LichSuThanhToan> findByHoaDon_IdOrderByThoiGianAscIdAsc(Integer idHoaDon);

    /** Các lần trả tiền của mọi hóa đơn thuộc một ca (tổng kết giao ca: tách tiền mặt / chuyển khoản). */
    List<LichSuThanhToan> findByHoaDon_IdGiaoCa(Integer idGiaoCa);

    /** Các lần thanh toán của hóa đơn có trạng thái `trangThai`, tạo trong khoảng [tu, den] (thống kê theo phương thức). */
    List<LichSuThanhToan> findByHoaDon_TrangThaiAndHoaDon_NgayTaoBetween(Integer trangThai, java.time.LocalDateTime tu, java.time.LocalDateTime den);
}
