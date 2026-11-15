package com.footstyle.demo.repository;

import com.footstyle.demo.entity.HoaDonChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Integer> {
    List<HoaDonChiTiet> findByHoaDon_IdOrderByIdAsc(Integer idHoaDon);

    /** Chi tiết của các hóa đơn có trạng thái `trangThai`, tạo trong khoảng [tu, den] (thống kê sản phẩm bán chạy). */
    List<HoaDonChiTiet> findByHoaDon_TrangThaiAndHoaDon_NgayTaoBetween(Integer trangThai, java.time.LocalDateTime tu, java.time.LocalDateTime den);

    /** Tổng số lượng từng biến thể trong các hóa đơn loại `loaiDon` đang ở trạng thái `trangThai`: [idBienThe, tổng]. */
    @Query("select ct.sanPhamChiTiet.id, sum(ct.soLuong) from HoaDonChiTiet ct where ct.hoaDon.loaiDon = :loaiDon "
            + "and ct.hoaDon.trangThai = :trangThai and ct.sanPhamChiTiet.id in :ids group by ct.sanPhamChiTiet.id")
    List<Object[]> tongSoLuongTheoBienThe(@Param("loaiDon") Integer loaiDon, @Param("trangThai") Integer trangThai,
                                          @Param("ids") java.util.Collection<Integer> ids);
}
