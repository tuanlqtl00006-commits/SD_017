package com.footstyle.demo.repository;

import com.footstyle.demo.entity.SanPham;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    List<SanPham> findAllByOrderByIdDesc();

    boolean existsByMaSanPhamIgnoreCase(String maSanPham);

    /** Sản phẩm cùng danh mục + thương hiệu (dùng để tìm sản phẩm trùng tên khi thêm mới). */
    List<SanPham> findByDanhMucIdAndThuongHieuId(Integer idDanhMuc, Integer idThuongHieu);

    @Query("select s.maSanPham from SanPham s")
    List<String> findAllMaSanPham();
}
