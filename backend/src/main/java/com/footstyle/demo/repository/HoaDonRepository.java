package com.footstyle.demo.repository;

import com.footstyle.demo.entity.HoaDon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer>, JpaSpecificationExecutor<HoaDon> {
    Page<HoaDon> findByTrangThai(Integer trangThai, Pageable pageable);

    /**
     * Thống kê mua hàng theo từng khách: [id khách hàng, số đơn, ngày tạo đơn gần nhất].
     * Không tính các đơn có trạng thái truyền vào (dùng để bỏ đơn đã hủy).
     */
    @Query("SELECT h.khachHang.id, COUNT(h), MAX(h.ngayTao) FROM HoaDon h "
            + "WHERE h.khachHang IS NOT NULL AND (h.trangThai IS NULL OR h.trangThai <> :boQuaTrangThai) "
            + "GROUP BY h.khachHang.id")
    java.util.List<Object[]> thongKeMuaHangTheoKhach(@Param("boQuaTrangThai") Integer boQuaTrangThai);

    /** Tên phương thức thanh toán (bảng phuong_thuc_thanh_toan chưa có entity riêng). */
    @Query(value = "SELECT ten_phuong_thuc FROM phuong_thuc_thanh_toan WHERE id = :id", nativeQuery = true)
    String findTenPhuongThucThanhToan(@Param("id") Integer id);
}
