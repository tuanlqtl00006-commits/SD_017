package com.footstyle.demo.repository;

import com.footstyle.demo.entity.ThanhVienHoiThoai;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThanhVienHoiThoaiRepository extends JpaRepository<ThanhVienHoiThoai, Integer> {
    List<ThanhVienHoiThoai> findByIdNhanVien(Integer idNhanVien);

    List<ThanhVienHoiThoai> findByIdHoiThoai(Integer idHoiThoai);

    Optional<ThanhVienHoiThoai> findFirstByIdHoiThoaiAndIdNhanVien(Integer idHoiThoai, Integer idNhanVien);
}
