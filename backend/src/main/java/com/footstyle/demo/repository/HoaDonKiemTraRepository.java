package com.footstyle.demo.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;





@Repository
public class HoaDonKiemTraRepository {

    @PersistenceContext
    private EntityManager em;

    public long demTheoNhanVien(Integer idNhanVien) {
        return dem("SELECT COUNT(*) FROM hoa_don WHERE id_nhan_vien = ?1", idNhanVien);
    }

    public long demTheoPhieuGiamGia(Integer idPhieuGiamGia) {
        return dem("SELECT COUNT(*) FROM hoa_don WHERE id_phieu_giam_gia = ?1", idPhieuGiamGia);
    }

    private long dem(String sql, Integer id) {
        Object result = em.createNativeQuery(sql).setParameter(1, id).getSingleResult();
        return ((Number) result).longValue();
    }
}
