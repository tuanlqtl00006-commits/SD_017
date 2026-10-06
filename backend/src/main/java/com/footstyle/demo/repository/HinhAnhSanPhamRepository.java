package com.footstyle.demo.repository;

import com.footstyle.demo.entity.HinhAnhSanPham;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HinhAnhSanPhamRepository extends JpaRepository<HinhAnhSanPham, Integer> {

    
    List<HinhAnhSanPham> findBySanPhamId(Integer idSanPham);

    
    List<HinhAnhSanPham> findByTrangThaiOrderByLaAnhChinhDescIdAsc(Integer trangThai);
}
