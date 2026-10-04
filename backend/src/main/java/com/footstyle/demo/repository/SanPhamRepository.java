package com.footstyle.demo.repository;

import com.footstyle.demo.entity.SanPham;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    List<SanPham> findAllByOrderByIdDesc();

    boolean existsByMaSanPhamIgnoreCase(String maSanPham);

    @Query("select s.maSanPham from SanPham s")
    List<String> findAllMaSanPham();
}
