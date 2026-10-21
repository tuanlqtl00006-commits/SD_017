package com.footstyle.demo.repository;

import com.footstyle.demo.entity.PhuongThucThanhToan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhuongThucThanhToanRepository extends JpaRepository<PhuongThucThanhToan, Integer> {
    Optional<PhuongThucThanhToan> findFirstByMaPhuongThuc(String maPhuongThuc);
}
