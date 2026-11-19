package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Phí giao hàng theo quãng đường (API bản đồ OpenStreetMap), dùng bản đồ giả nên không gọi mạng. */
class PhiVanChuyenServiceTest {

    /** Bản đồ giả: địa chỉ nào cũng có tọa độ, quãng đường luôn bằng soKm. */
    static PhiVanChuyenService.BanDo banDoGia(double soKm) {
        return new PhiVanChuyenService.BanDo() {
            @Override
            public Optional<double[]> toaDo(String diaChi) {
                return Optional.of(new double[] {21.0, 105.8});
            }

            @Override
            public Optional<Double> soKm(double[] tu, double[] den) {
                return Optional.of(soKm);
            }
        };
    }

    private final CaiDatVanChuyenService caiDat = CaiDatVanChuyenService.trongBoNho(true, 2_000_000L, 25_000L);

    private PhiVanChuyenService dichVu(PhiVanChuyenService.BanDo banDo) {
        return new PhiVanChuyenService(caiDat, banDo, "123 Đường Cầu Diễn, Bắc Từ Liêm, Hà Nội", 10, 50, 1000, true);
    }

    @Test
    void phiTheoKm_10kmDauLaPhiCaiDat_moi50kmCongThem1000() {
        PhiVanChuyenService s = dichVu(banDoGia(0));
        assertEquals(25_000L, s.phiTheoKm(3.2), "trong 10 km đầu");
        assertEquals(25_000L, s.phiTheoKm(10));
        assertEquals(26_000L, s.phiTheoKm(10.1), "vừa quá 10 km: thêm 1 bước");
        assertEquals(26_000L, s.phiTheoKm(60));
        assertEquals(27_000L, s.phiTheoKm(60.5));
        assertEquals(55_000L, s.phiTheoKm(1497.4), "Hà Nội -> TP HCM khoảng 1.497 km đường đi");
    }

    @Test
    void tinh_coBanDo_traSoKmVaNguonBanDo() {
        PhiVanChuyenService.KetQua kq = dichVu(banDoGia(35.6)).tinh("Tỉnh Bắc Ninh", "Phường Kinh Bắc", "Số 1 Lý Thái Tổ");
        assertEquals(26_000L, kq.phi());
        assertEquals(35.6, kq.soKm());
        assertEquals(PhiVanChuyenService.NGUON_BAN_DO, kq.nguon());
    }

    @Test
    void tinh_banDoKhongTimThay_tamTinhTheoBangTinh() {
        PhiVanChuyenService.BanDo hong = new PhiVanChuyenService.BanDo() {
            @Override
            public Optional<double[]> toaDo(String diaChi) {
                return Optional.empty();
            }

            @Override
            public Optional<Double> soKm(double[] tu, double[] den) {
                return Optional.empty();
            }
        };
        PhiVanChuyenService.KetQua kq = dichVu(hong).tinh("Thành phố Hồ Chí Minh", "Phường Sài Gòn", null);
        assertEquals(PhiVanChuyenTinh.phi("Thành phố Hồ Chí Minh"), kq.phi());
        assertNull(kq.soKm());
        assertEquals(PhiVanChuyenService.NGUON_BANG_TINH, kq.nguon());
    }

    @Test
    void tinh_tatBanDo_khongGoiBanDo() {
        List<String> daGoi = new ArrayList<>();
        PhiVanChuyenService.BanDo ghi = new PhiVanChuyenService.BanDo() {
            @Override
            public Optional<double[]> toaDo(String diaChi) {
                daGoi.add(diaChi);
                return Optional.empty();
            }

            @Override
            public Optional<Double> soKm(double[] tu, double[] den) {
                return Optional.empty();
            }
        };
        PhiVanChuyenService tat = new PhiVanChuyenService(caiDat, ghi, "Hà Nội", 10, 50, 1000, false);
        assertEquals(PhiVanChuyenService.NGUON_BANG_TINH, tat.tinh("Thành phố Hà Nội", "Phường Hà Đông", null).nguon());
        assertTrue(daGoi.isEmpty());
    }

    @Test
    void tinh_nhoKetQua_diaChiGiongNhauKhongGoiLai() {
        List<String> daGoi = new ArrayList<>();
        PhiVanChuyenService.BanDo dem = new PhiVanChuyenService.BanDo() {
            @Override
            public Optional<double[]> toaDo(String diaChi) {
                daGoi.add(diaChi);
                return Optional.of(new double[] {21, 105});
            }

            @Override
            public Optional<Double> soKm(double[] tu, double[] den) {
                daGoi.add("soKm");
                return Optional.of(8.0);
            }
        };
        PhiVanChuyenService s = dichVu(dem);
        s.tinh("Thành phố Hà Nội", "Phường Hà Đông", null);
        int lan1 = daGoi.size();
        s.tinh("Thành phố Hà Nội", "Phường Hà Đông", null);
        assertEquals(lan1, daGoi.size(), "lần 2 lấy từ bộ nhớ, không gọi bản đồ");
    }

    @Test
    void tinhTheoDiaChi_tachTinhVaPhuong() {
        List<String> daTim = new ArrayList<>();
        PhiVanChuyenService.BanDo ghi = new PhiVanChuyenService.BanDo() {
            @Override
            public Optional<double[]> toaDo(String diaChi) {
                daTim.add(diaChi);
                return Optional.of(new double[] {21, 105});
            }

            @Override
            public Optional<Double> soKm(double[] tu, double[] den) {
                return Optional.of(5.0);
            }
        };
        PhiVanChuyenService.KetQua kq = dichVu(ghi).tinhTheoDiaChi("Số 15 Trần Phú, Phường Hà Đông, Thành phố Hà Nội");
        assertEquals(25_000L, kq.phi());
        assertEquals("Phường Hà Đông, Thành phố Hà Nội", daTim.get(0), "tìm theo phường + tỉnh trước");
        assertNull(dichVu(ghi).tinhTheoDiaChi("Số 15 Trần Phú, khu phố 3"), "không nhận ra tỉnh: null để nơi gọi giữ phí cũ");
    }

    @Test
    void ungVienDiaChi_cuaHang_boSoNhaVaBotDan() {
        List<String> ds = PhiVanChuyenService.ungVienDiaChi("123 Đường Cầu Diễn, Bắc Từ Liêm, Hà Nội");
        assertEquals("123 Đường Cầu Diễn, Bắc Từ Liêm, Hà Nội", ds.get(0));
        assertTrue(ds.contains("Đường Cầu Diễn, Hà Nội"), ds.toString());
        assertEquals("Hà Nội", ds.get(ds.size() - 1));
    }
}
