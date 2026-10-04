package com.footstyle.demo.repository;

import com.footstyle.demo.entity.PhieuGiamGia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, Integer> {

    List<PhieuGiamGia> findAllByOrderByIdDesc();

    boolean existsByMaPhieuIgnoreCase(String maPhieu);
}
