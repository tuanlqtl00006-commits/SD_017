package com.footstyle.demo.repository;

import com.footstyle.demo.entity.HoaDon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer>, JpaSpecificationExecutor<HoaDon> {
    Page<HoaDon> findByTrangThai(Integer trangThai, Pageable pageable);

    /** Tên phương thức thanh toán (bảng phuong_thuc_thanh_toan chưa có entity riêng). */
    @Query(value = "SELECT ten_phuong_thuc FROM phuong_thuc_thanh_toan WHERE id = :id", nativeQuery = true)
    String findTenPhuongThucThanhToan(@Param("id") Integer id);
}
