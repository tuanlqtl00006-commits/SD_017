package com.footstyle.demo.repository;

import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DotGiamGiaChiTietRepository extends JpaRepository<DotGiamGiaChiTiet, Long> {
    List<DotGiamGiaChiTiet> findByDotGiamGiaId(Long idDotGiamGia);
    long countByDotGiamGiaId(Long idDotGiamGia);
    List<DotGiamGiaChiTiet> findByIdSanPhamChiTiet(Long idSanPhamChiTiet);
    boolean existsByDotGiamGiaIdAndIdSanPhamChiTiet(Long idDotGiamGia, Long idSanPhamChiTiet);
    void deleteByDotGiamGiaId(Long idDotGiamGia);

    /** Biến thể thuộc các đợt đang bật mà chưa kết thúc tính đến thời điểm tu (đang diễn ra + sắp diễn ra). */
    @Query("select c from DotGiamGiaChiTiet c join fetch c.dotGiamGia d "
            + "where d.trangThai = 1 and (c.trangThai is null or c.trangThai = 1) and d.ngayKetThuc >= :tu "
            + "order by d.ngayBatDau, d.id")
    List<DotGiamGiaChiTiet> findApDung(@Param("tu") LocalDateTime tu);
}
