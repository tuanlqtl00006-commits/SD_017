package com.footstyle.demo.repository;

import com.footstyle.demo.entity.LichLamViec;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LichLamViecRepository extends JpaRepository<LichLamViec, Integer> {
    List<LichLamViec> findByNgayLamBetween(LocalDate tuNgay, LocalDate denNgay);

    List<LichLamViec> findByNhanVienIdAndNgayLamBetween(Integer idNhanVien, LocalDate tuNgay, LocalDate denNgay);

    List<LichLamViec> findByNhanVienIdAndNgayLam(Integer idNhanVien, LocalDate ngayLam);
}
