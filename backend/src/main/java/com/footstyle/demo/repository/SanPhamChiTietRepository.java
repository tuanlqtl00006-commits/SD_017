package com.footstyle.demo.repository;

import com.footstyle.demo.entity.SanPhamChiTiet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Integer> {

    List<SanPhamChiTiet> findAllByOrderByIdDesc();

    List<SanPhamChiTiet> findBySanPhamIdOrderByIdDesc(Integer idSanPham);

    boolean existsByMaSpctIgnoreCase(String maSpct);

    
    boolean existsBySanPhamIdAndMauSacIdAndTrongLuongIdAndChuViIdAndIdNot(
            Integer idSanPham, Integer idMauSac, Integer idTrongLuong, Integer idChuVi, Integer id);
}
