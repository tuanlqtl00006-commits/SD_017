package com.footstyle.demo.repository;

import com.footstyle.demo.entity.ThuocTinh;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;


@NoRepositoryBean
public interface ThuocTinhRepository<T extends ThuocTinh> extends JpaRepository<T, Integer> {

    List<T> findAllByOrderByIdDesc();

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByTenIgnoreCaseAndIdNot(String ten, Integer id);
}
