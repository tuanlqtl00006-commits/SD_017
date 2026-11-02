package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.repository.HoaDonRepository;
import com.footstyle.demo.security.XacThuc;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ChuyenKhoanQuayServiceTest {

    private final CaLamMau m = new CaLamMau();
    private final HoaDon cho = hoaDonCho();
    private final ChuyenKhoanQuayService service = new ChuyenKhoanQuayService(
            KhoGia.tao(HoaDonRepository.class, (ten, a) -> "findById".equals(ten) ? Optional.ofNullable(cho.getId().equals(a[0]) ? cho : null) : KhoGia.KHONG_CO));

    @AfterEach
    void don() {
        XacThuc.xoa();
    }

    private HoaDon hoaDonCho() {
        HoaDon h = new HoaDon();
        h.setId(5);
        h.setMaHoaDon("HD961");
        h.setLoaiDon(HoaDon.TAI_QUAY);
        h.setTrangThai(HoaDon.CHO_XAC_NHAN);
        h.setNhanVien(m.bao);
        return h;
    }

    @Test
    void tatGiaLap_404() {
        CaLamMau.dangNhap(m.bao);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, () -> service.taoYeuCau(5, 100_000L)).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, () -> service.xemChoNganHang("HD961")).getStatus());
    }

    @Test
    void khachQuetQrXacNhan_quayThayDaNhanTien() {
        service.datGiaLap(true);
        CaLamMau.dangNhap(m.bao);
        Map<String, Object> yc = service.taoYeuCau(5, 292_500L);
        assertEquals(false, yc.get("daNhan"));
        // trang ngân hàng giả (công khai) dùng chung dạng dữ liệu với đơn online
        Map<String, Object> nh = service.xemChoNganHang("HD961");
        assertEquals(292_500L, nh.get("conLai"));
        assertEquals(true, nh.get("laChuyenKhoan"));
        service.xacNhanTuNganHang("HD961");
        Map<String, Object> tt = service.trangThai(5);
        assertEquals(true, tt.get("daNhan"));
        assertTrue(((String) tt.get("maGiaoDich")).startsWith("GIALAP-HD961-"));
        // cùng số tiền thì giữ trạng thái đã nhận; đổi số tiền thì phải quét lại
        assertEquals(true, service.taoYeuCau(5, 292_500L).get("daNhan"));
        Map<String, Object> moi = service.taoYeuCau(5, 300_000L);
        assertEquals(false, moi.get("daNhan"));
        assertNull(moi.get("maGiaoDich"));
        service.xoa("HD961");
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, () -> service.xemChoNganHang("HD961")).getStatus());
    }

    @Test
    void hoaDonCuaNguoiKhac_hoacSoTienSai_bichan() {
        service.datGiaLap(true);
        CaLamMau.dangNhap(m.anh);
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class, () -> service.taoYeuCau(5, 100_000L)).getStatus());
        CaLamMau.dangNhap(m.bao);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class, () -> service.taoYeuCau(5, 0L)).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, () -> service.taoYeuCau(99, 100_000L)).getStatus());
    }
}
