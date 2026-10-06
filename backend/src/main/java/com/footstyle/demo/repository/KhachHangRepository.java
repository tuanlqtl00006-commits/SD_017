package com.footstyle.demo.repository;

import com.footstyle.demo.entity.KhachHang;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {

    List<KhachHang> findByTrangThaiOrderByHoTenAsc(Integer trangThai);
}
