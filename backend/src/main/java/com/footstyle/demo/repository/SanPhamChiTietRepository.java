package com.footstyle.demo.repository;

import com.footstyle.demo.entity.SanPhamChiTiet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Integer> {

    List<SanPhamChiTiet> findAllByOrderByIdDesc();

    List<SanPhamChiTiet> findBySanPhamIdOrderByIdDesc(Integer idSanPham);

    boolean existsByMaSpctIgnoreCase(String maSpct);

    /** Dùng khi sửa (đổi sản phẩm làm đổi mã): có biến thể KHÁC (id khác) đang dùng mã này chưa. */
    boolean existsByMaSpctIgnoreCaseAndIdNot(String maSpct, Integer id);

    /** Cùng sản phẩm + màu + trọng lượng + chu vi (bỏ qua chính biến thể đang sửa) là biến thể trùng. */
    boolean existsBySanPhamIdAndMauSacIdAndTrongLuongIdAndChuViIdAndIdNot(
            Integer idSanPham, Integer idMauSac, Integer idTrongLuong, Integer idChuVi, Integer id);
}
