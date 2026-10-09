package com.footstyle.demo.repository;

import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DotGiamGiaChiTietRepository extends JpaRepository<DotGiamGiaChiTiet, Long> {
    List<DotGiamGiaChiTiet> findByDotGiamGiaId(Long idDotGiamGia);
    boolean existsByDotGiamGiaIdAndIdSanPhamChiTiet(Long idDotGiamGia, Long idSanPhamChiTiet);

    // Lấy mức giảm giá lớn nhất (Tự động tính gg max) cho 1 sản phẩm chi tiết khi bị trùng nhiều đợt giảm giá cùng lúc
    @org.springframework.data.jpa.repository.Query(
        "SELECT MAX(c.phanTramGiamBienThe) FROM DotGiamGiaChiTiet c " +
        "WHERE c.idSanPhamChiTiet = :idSanPhamChiTiet " +
        "AND c.trangThai = 1 " +
        "AND c.dotGiamGia.trangThai = 1 " +
        "AND CURRENT_TIMESTAMP BETWEEN c.dotGiamGia.ngayBatDau AND c.dotGiamGia.ngayKetThuc"
    )
    Integer findMaxGiamGiaBySanPhamChiTietId(@org.springframework.data.repository.query.Param("idSanPhamChiTiet") Long idSanPhamChiTiet);
}
