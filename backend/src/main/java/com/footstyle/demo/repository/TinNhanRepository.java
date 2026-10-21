package com.footstyle.demo.repository;

import com.footstyle.demo.entity.TinNhan;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TinNhanRepository extends JpaRepository<TinNhan, Integer> {
    /** Tin mới hơn mốc `sauId` (giao diện hỏi lại định kỳ chỉ lấy phần mới). */
    List<TinNhan> findTop200ByIdHoiThoaiAndIdGreaterThanOrderByIdAsc(Integer idHoiThoai, Integer sauId);

    /** 100 tin gần nhất (lần mở đầu tiên). */
    List<TinNhan> findTop100ByIdHoiThoaiOrderByIdDesc(Integer idHoiThoai);

    Optional<TinNhan> findFirstByIdHoiThoaiOrderByIdDesc(Integer idHoiThoai);

    /** Số tin chưa đọc của một nhân viên trong hội thoại: sau mốc đã đọc và không phải tin chính mình gửi. */
    @Query("select count(t) from TinNhan t where t.idHoiThoai = :idHoiThoai and t.id > :sauId "
            + "and (t.idNhanVien is null or t.idNhanVien <> :idNhanVien)")
    long demChuaDoc(@Param("idHoiThoai") Integer idHoiThoai, @Param("sauId") Integer sauId, @Param("idNhanVien") Integer idNhanVien);

    /** Chống gửi dồn dập: số tin của khách trong hội thoại kể từ một mốc. */
    long countByIdHoiThoaiAndLoaiNguoiGuiAndThoiGianAfter(Integer idHoiThoai, Integer loaiNguoiGui, LocalDateTime moc);
}
