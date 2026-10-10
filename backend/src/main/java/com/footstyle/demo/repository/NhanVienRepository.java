package com.footstyle.demo.repository;

import com.footstyle.demo.entity.NhanVien;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {

    List<NhanVien> findAllByOrderByIdDesc();

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    boolean existsBySdtAndIdNot(String sdt, Integer id);

    boolean existsByCccdAndIdNot(String cccd, Integer id);

    /** Số nhân viên đang hoạt động thuộc một vai trò (dùng để giữ lại ít nhất một quản lý). */
    long countByVaiTroIdAndTrangThai(Integer idVaiTro, Integer trangThai);

    @Query("select n.maNhanVien from NhanVien n")
    List<String> findAllMaNhanVien();
}
