package com.footstyle.demo.repository;

import com.footstyle.demo.entity.DiaChiKhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaChiKhachHangRepository extends JpaRepository<DiaChiKhachHang, Long> {
    List<DiaChiKhachHang> findByKhachHangId(Long idKhachHang);
}
