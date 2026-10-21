package com.footstyle.demo.repository;

import com.footstyle.demo.entity.HoaDon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer>, JpaSpecificationExecutor<HoaDon> {
    Optional<HoaDon> findFirstByMaHoaDonIgnoreCase(String maHoaDon);

    Page<HoaDon> findByTrangThai(Integer trangThai, Pageable pageable);

    /** Số đơn (chưa hủy) của một khách đã dùng một phiếu: kiểm tra giới hạn số lần dùng mỗi khách. */
    long countByKhachHang_IdAndPhieuGiamGia_IdAndTrangThaiNot(Integer idKhachHang, Integer idPhieuGiamGia, Integer trangThai);

    /** Hóa đơn bán trong một ca (giai đoạn 3: hoa_don.id_giao_ca). */
    List<HoaDon> findByIdGiaoCaOrderByNgayTaoAsc(Integer idGiaoCa);

    /** Mọi mã hóa đơn (để cấp mã mới = số lớn nhất + 1). */
    @Query("select h.maHoaDon from HoaDon h")
    List<String> findAllMaHoaDon();

    /**
     * Mỗi khách đã mua: { idKhachHang, số đơn có trạng thái trangThai, ngày tạo đơn gần nhất }.
     * Dùng cho bảng chọn khách khi tặng phiếu cá nhân (cột "Tổng đơn", "Mua gần nhất"); truyền HoaDon.HOAN_THANH.
     */
    @Query("SELECT h.khachHang.id, COUNT(h), MAX(h.ngayTao) FROM HoaDon h "
            + "WHERE h.khachHang IS NOT NULL AND h.trangThai = :trangThai GROUP BY h.khachHang.id")
    List<Object[]> thongKeMuaHangTheoKhach(@Param("trangThai") Integer trangThai);

    /** Tên phương thức thanh toán (bảng phuong_thuc_thanh_toan chưa có entity riêng). */
    @Query(value = "SELECT ten_phuong_thuc FROM phuong_thuc_thanh_toan WHERE id = :id", nativeQuery = true)
    String findTenPhuongThucThanhToan(@Param("id") Integer id);

    /** Hóa đơn có trạng thái `trangThai`, tạo trong khoảng [tu, den] (thống kê). */
    List<HoaDon> findByTrangThaiAndNgayTaoBetween(Integer trangThai, java.time.LocalDateTime tu, java.time.LocalDateTime den);
}
