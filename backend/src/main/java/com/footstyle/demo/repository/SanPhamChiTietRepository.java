package com.footstyle.demo.repository;

import com.footstyle.demo.entity.SanPhamChiTiet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Integer> {

    /**
     * Bán hàng tại quầy: trừ tồn kho CÓ ĐIỀU KIỆN trong một câu lệnh (chỉ trừ khi còn đủ hàng). Trả về số dòng đã đổi:
     * 0 = không đủ hàng (2 máy cùng bán món cuối thì máy sau nhận 0 và hủy giao dịch, tồn kho không bao giờ âm).
     */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update SanPhamChiTiet c set c.soLuongTon = c.soLuongTon - :soLuong "
            + "where c.id = :id and c.soLuongTon >= :soLuong")
    int truTonKho(@org.springframework.data.repository.query.Param("id") Integer id,
                  @org.springframework.data.repository.query.Param("soLuong") int soLuong);

    java.util.Optional<SanPhamChiTiet> findFirstByMaSpctIgnoreCase(String maSpct);

    List<SanPhamChiTiet> findAllByOrderByIdDesc();

    List<SanPhamChiTiet> findBySanPhamIdOrderByIdDesc(Integer idSanPham);

    boolean existsByMaSpctIgnoreCase(String maSpct);

    boolean existsBySanPhamId(Integer idSanPham);

    /** Biến thể đã có của sản phẩm với đúng bộ màu + trọng lượng + chu vi (dùng khi thêm: trùng thì hỏi cập nhật). */
    List<SanPhamChiTiet> findBySanPhamIdAndMauSacIdAndTrongLuongIdAndChuViId(
            Integer idSanPham, Integer idMauSac, Integer idTrongLuong, Integer idChuVi);

    /** Dùng khi sửa (đổi sản phẩm làm đổi mã): có biến thể KHÁC (id khác) đang dùng mã này chưa. */
    boolean existsByMaSpctIgnoreCaseAndIdNot(String maSpct, Integer id);

    /** Cùng sản phẩm + màu + trọng lượng + chu vi (bỏ qua chính biến thể đang sửa) là biến thể trùng. */
    boolean existsBySanPhamIdAndMauSacIdAndTrongLuongIdAndChuViIdAndIdNot(
            Integer idSanPham, Integer idMauSac, Integer idTrongLuong, Integer idChuVi, Integer id);

    /** Biến thể đang bán còn tồn kho ít (<= nguong), ít nhất lên trước (thống kê "sắp hết hàng"). */
    List<SanPhamChiTiet> findByTrangThaiAndSoLuongTonLessThanEqualOrderBySoLuongTonAscIdAsc(Integer trangThai, Integer nguong);
}
