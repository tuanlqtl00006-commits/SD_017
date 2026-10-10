package com.footstyle.demo.repository;

import com.footstyle.demo.entity.ViTri;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViTriRepository extends JpaRepository<ViTri, Integer> {

    List<ViTri> findByTrangThaiOrderByIdAsc(Integer trangThai);

    boolean existsByTenViTriIgnoreCase(String tenViTri);
}
