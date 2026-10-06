package com.footstyle.demo.service;

import com.footstyle.demo.entity.ThuocTinh;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.ThuocTinhRepository;







final class ThuocTinhChon {

    private ThuocTinhChon() {
    }

    static <T extends ThuocTinh> T chon(ThuocTinhRepository<T> repo, Integer id, T hienTai, String nhan) {
        if (id == null) {
            throw ApiException.badRequest("Chọn " + nhan + ".");
        }
        T moi = repo.findById(id).orElseThrow(() -> ApiException.badRequest("Giá trị " + nhan + " không tồn tại."));
        boolean giuNguyen = hienTai != null && hienTai.getId().equals(moi.getId());
        boolean hoatDong = moi.getTrangThai() != null && moi.getTrangThai() == ThuocTinh.HOAT_DONG;
        if (!giuNguyen && !hoatDong) {
            throw ApiException.badRequest("Giá trị " + nhan + " đã ngưng hoạt động, hãy chọn giá trị khác.");
        }
        return moi;
    }
}
