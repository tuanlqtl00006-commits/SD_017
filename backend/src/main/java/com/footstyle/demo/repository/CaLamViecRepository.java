package com.footstyle.demo.repository;

import com.footstyle.demo.entity.CaLamViec;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CaLamViecRepository extends JpaRepository<CaLamViec, Integer> {
    List<CaLamViec> findAllByOrderByGioBatDauAscIdAsc();

    boolean existsByTenCaIgnoreCaseAndIdNot(String tenCa, Integer id);

    @Query("select c.maCa from CaLamViec c")
    List<String> findAllMaCa();
}
