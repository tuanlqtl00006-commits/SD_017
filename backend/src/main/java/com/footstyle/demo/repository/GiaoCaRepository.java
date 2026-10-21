package com.footstyle.demo.repository;

import com.footstyle.demo.entity.GiaoCa;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GiaoCaRepository extends JpaRepository<GiaoCa, Integer> {
    /** Ca đang mở (trang_thai = 1) của một nhân viên. */
    Optional<GiaoCa> findFirstByNhanVienIdAndTrangThai(Integer idNhanVien, Integer trangThai);

    /** Ca giao gần nhất (để gợi ý tiền đầu ca = tiền bàn giao của ca trước). */
    Optional<GiaoCa> findFirstByTrangThaiOrderByThoiGianKetCaDescIdDesc(Integer trangThai);

    List<GiaoCa> findByThoiGianNhanCaBetweenOrderByThoiGianNhanCaDesc(LocalDateTime tu, LocalDateTime den);

    List<GiaoCa> findByNhanVienIdAndThoiGianNhanCaBetweenOrderByThoiGianNhanCaDesc(Integer idNhanVien, LocalDateTime tu, LocalDateTime den);

    boolean existsByLichLamViecId(Integer idLichLamViec);

    @Query("select g.maGiaoCa from GiaoCa g")
    List<String> findAllMaGiaoCa();
}
