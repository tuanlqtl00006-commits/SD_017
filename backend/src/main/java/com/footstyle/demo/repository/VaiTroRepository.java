package com.footstyle.demo.repository;

import com.footstyle.demo.entity.VaiTro;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VaiTroRepository extends JpaRepository<VaiTro, Integer> {

    List<VaiTro> findByTrangThaiOrderByIdAsc(Integer trangThai);
}
