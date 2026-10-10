package com.footstyle.demo.repository;

import com.footstyle.demo.entity.DiaChiNhanVien;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaChiNhanVienRepository extends JpaRepository<DiaChiNhanVien, Integer> {

    /** Địa chỉ chính đứng đầu, sau đó theo thứ tự thêm vào. */
    List<DiaChiNhanVien> findByNhanVienIdOrderByMacDinhDescIdAsc(Integer idNhanVien);
}
