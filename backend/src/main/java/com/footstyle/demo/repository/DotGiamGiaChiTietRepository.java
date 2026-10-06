package com.footstyle.demo.repository;

import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DotGiamGiaChiTietRepository extends JpaRepository<DotGiamGiaChiTiet, Long> {
    List<DotGiamGiaChiTiet> findByDotGiamGiaId(Long idDotGiamGia);
    boolean existsByDotGiamGiaIdAndIdSanPhamChiTiet(Long idDotGiamGia, Long idSanPhamChiTiet);
}
