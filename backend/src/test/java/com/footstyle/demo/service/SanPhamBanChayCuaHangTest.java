package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.KhoGia;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.HoaDonChiTiet;
import com.footstyle.demo.entity.SanPham;
import com.footstyle.demo.entity.SanPhamChiTiet;
import com.footstyle.demo.entity.ThuongHieu;
import com.footstyle.demo.repository.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Sản phẩm bán chạy ở trang chủ (GET /cua-hang/ban-chay): số thật từ đơn "Đã hoàn thành" trong 30 ngày, không còn số bịa. */
class SanPhamBanChayCuaHangTest {

    private final List<SanPham> sanPhams = new ArrayList<>();
    private final List<HoaDonChiTiet> chiTiets = new ArrayList<>();
    private Object[] thamSoHoi;

    private final CuaHangService service = new CuaHangService(
            KhoGia.tao(SanPhamRepository.class, (t, a) -> "findAll".equals(t) && a.length == 0 ? sanPhams : KhoGia.KHONG_CO),
            KhoGia.khongDung(SanPhamChiTietRepository.class),
            KhoGia.khongDung(HinhAnhSanPhamRepository.class), KhoGia.khongDung(DanhMucRepository.class),
            KhoGia.khongDung(ThuongHieuRepository.class), KhoGia.khongDung(XuatXuRepository.class),
            KhoGia.khongDung(ChatLieuRepository.class), KhoGia.khongDung(DoCungRepository.class),
            KhoGia.khongDung(DiemCanBangRepository.class), KhoGia.khongDung(MauSacRepository.class),
            KhoGia.khongDung(TrongLuongRepository.class), KhoGia.khongDung(ChuViRepository.class),
            KhoGia.khongDung(HoaDonRepository.class),
            KhoGia.tao(HoaDonChiTietRepository.class, (t, a) -> {
                if (!"findByHoaDon_TrangThaiAndHoaDon_NgayTaoBetween".equals(t)) return KhoGia.KHONG_CO;
                thamSoHoi = a;
                return chiTiets;
            }),
            KhoGia.khongDung(LichSuHoaDonRepository.class), KhoGia.khongDung(LichSuThanhToanRepository.class),
            KhoGia.khongDung(PhieuGiamGiaRepository.class), KhoGia.khongDung(PhieuGiamGiaKhachHangRepository.class),
            KhoGia.khongDung(KhachHangRepository.class), null);

    private SanPham sp(int id, int trangThai, ThuongHieu th) {
        SanPham s = new SanPham();
        s.setId(id);
        s.setMaSanPham("SP" + id);
        s.setTenSanPham("Vợt " + id);
        s.setTrangThai(trangThai);
        s.setThuongHieu(th);
        sanPhams.add(s);
        return s;
    }

    private void ban(SanPham s, int idBienThe, int soLuong, long donGia, int idHoaDon) {
        SanPhamChiTiet c = new SanPhamChiTiet();
        c.setId(idBienThe);
        c.setMaSpct(s.getMaSanPham() + "-" + idBienThe);
        c.setSanPham(s);
        HoaDon hd = new HoaDon();
        hd.setId(idHoaDon);
        HoaDonChiTiet ct = new HoaDonChiTiet();
        ct.setHoaDon(hd);
        ct.setSanPhamChiTiet(c);
        ct.setSoLuong(soLuong);
        ct.setDonGia(BigDecimal.valueOf(donGia));
        ct.setThanhTien(BigDecimal.valueOf(donGia * soLuong));
        chiTiets.add(ct);
    }

    @SuppressWarnings("unchecked")
    @Test
    void chiTinhDonHoanThanh30Ngay_xepTheoSoLuong_boSanPhamDaAn_toiDa4() {
        ThuongHieu ngung = new ThuongHieu();
        ngung.setTen("Hãng đã ngừng");
        ngung.setTrangThai(0);
        SanPham a = sp(1, 1, null);
        SanPham b = sp(2, 1, null);
        SanPham c = sp(3, 1, null);
        SanPham d = sp(4, 1, null);
        SanPham e = sp(5, 1, null);
        SanPham daAn = sp(6, 0, null);
        SanPham hangNgung = sp(7, 1, ngung);
        ban(a, 11, 3, 1_000_000, 100);
        ban(a, 12, 2, 1_000_000, 101); // 2 biến thể cùng sản phẩm: cộng dồn = 5
        ban(b, 21, 7, 500_000, 102);
        ban(c, 31, 4, 2_000_000, 103);
        ban(d, 41, 1, 900_000, 104);
        ban(e, 51, 1, 800_000, 105);
        ban(daAn, 61, 50, 100_000, 106);
        ban(hangNgung, 71, 40, 100_000, 107);

        Map<String, Object> kq = service.sanPhamBanChay();

        assertNotNull(thamSoHoi);
        assertEquals(HoaDon.HOAN_THANH, thamSoHoi[0], "chỉ tính đơn đã hoàn thành");
        Duration khoang = Duration.between((LocalDateTime) thamSoHoi[1], (LocalDateTime) thamSoHoi[2]);
        assertEquals(30, khoang.toDays(), "trong 30 ngày gần nhất");
        assertEquals(30, kq.get("soNgay"));

        List<Map<String, Object>> ds = (List<Map<String, Object>>) kq.get("ds");
        // 4 và 5 cùng bán 1 cái: 4 doanh thu 900.000 ₫ cao hơn 5 (800.000 ₫) nên đứng trước, 5 bị cắt vì chỉ lấy 4
        assertEquals(List.of(2, 1, 3, 4), ds.stream().map(m -> m.get("idSanPham")).toList(),
                "xếp theo số lượng (bằng nhau thì doanh thu cao hơn trước); bỏ sản phẩm đã ẩn / hãng ngừng; chỉ 4 sản phẩm");
        assertEquals(7L, ds.get(0).get("daBan"));
        assertEquals(5L, ds.get(1).get("daBan"), "cộng dồn các biến thể của cùng sản phẩm");
        assertEquals(3_500_000L, ds.get(0).get("doanhThu"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void chuaBanDuocCaiNao_traDanhSachRong() {
        sp(1, 1, null);
        List<Map<String, Object>> ds = (List<Map<String, Object>>) service.sanPhamBanChay().get("ds");
        assertTrue(ds.isEmpty(), "chưa bán được cái nào thì trang chủ ẩn khối bán chạy, không bịa số");
    }
}
