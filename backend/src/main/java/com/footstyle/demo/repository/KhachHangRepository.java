package com.footstyle.demo.repository;

import com.footstyle.demo.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {
    List<KhachHang> findByTenContainingIgnoreCaseOrSdtContainingIgnoreCaseOrEmailContainingIgnoreCase(String ten, String sdt, String email);
}
