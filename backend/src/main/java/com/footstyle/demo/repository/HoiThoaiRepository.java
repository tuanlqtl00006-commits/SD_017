package com.footstyle.demo.repository;

import com.footstyle.demo.entity.HoiThoai;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoiThoaiRepository extends JpaRepository<HoiThoai, Integer> {
    /** Hội thoại hỗ trợ khách gần nhất (mới cập nhật lên đầu). */
    List<HoiThoai> findTop200ByLoaiOrderByNgayCapNhatDesc(Integer loai);

    Optional<HoiThoai> findFirstByKhoaRieng(String khoaRieng);

    List<HoiThoai> findByIdIn(List<Integer> ids);

    /** Số hội thoại khách mở từ một SĐT kể từ một mốc (chống mở hàng loạt). */
    long countByLoaiAndSdtKhachAndNgayTaoAfter(Integer loai, String sdtKhach, LocalDateTime moc);

    long countByLoaiAndCheDo(Integer loai, Integer cheDo);
}
