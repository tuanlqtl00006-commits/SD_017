package com.footstyle.demo.repository;

import com.footstyle.demo.entity.HinhAnhSanPham;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HinhAnhSanPhamRepository extends JpaRepository<HinhAnhSanPham, Integer> {

    /** Tất cả ảnh (kể cả đã bỏ) của một sản phẩm, dùng khi đồng bộ lại danh sách ảnh lúc sửa. */
    List<HinhAnhSanPham> findBySanPhamId(Integer idSanPham);

    /** Ảnh đang dùng của tất cả sản phẩm, ảnh chính xếp trước. Dùng 1 câu truy vấn cho cả danh sách để tránh N+1. */
    List<HinhAnhSanPham> findByTrangThaiOrderByLaAnhChinhDescIdAsc(Integer trangThai);
}
