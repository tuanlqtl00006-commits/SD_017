package com.footstyle.demo.repository;

import com.footstyle.demo.entity.LichSuHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuHoaDonRepository extends JpaRepository<LichSuHoaDon, Integer> {
    /** Lịch sử của một hóa đơn, cũ trước mới sau (đúng thứ tự trên dòng thời gian). */
    List<LichSuHoaDon> findByHoaDon_IdOrderByThoiGianAscIdAsc(Integer idHoaDon);

    /**
     * Id các đơn GIAO HÀNG nhưng bán tại quầy (nhân viên bật "Giao hàng" ở màn bán quầy): dòng lịch sử lúc tạo đơn có chữ
     * "tại quầy". Thường rất ít đơn nên trả về cả danh sách một lần.
     */
    @Query("select distinct l.hoaDon.id from LichSuHoaDon l where l.hoaDon.loaiDon = 2 and l.moTa like :mau")
    List<Integer> idDonGiaoHangBanTaiQuay(@Param("mau") String mau);
}
