package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.service.PhieuMailNoiDung.Loai;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Theo dõi kết quả gửi từng lô mail phiếu giảm giá để giao diện báo rõ "đã gửi" hay "gửi lỗi + lý do". */
class MailKetQuaLoTest {

    private static PhieuGiamGia phieu(String ma) {
        PhieuGiamGia p = new PhieuGiamGia();
        p.setMaPhieu(ma);
        p.setTenPhieu("Tri ân");
        p.setLoaiGiam(PhieuGiamGia.LOAI_SO_TIEN);
        p.setGiaTriGiam(50_000L);
        p.setGiaTriDonHangToiThieu(300_000L);
        p.setNgayBatDau(LocalDateTime.of(2026, 10, 20, 0, 0));
        p.setNgayKetThuc(LocalDateTime.of(2026, 10, 30, 23, 59, 59));
        return p;
    }

    private static KhachHang khach(int id, String email) {
        KhachHang k = new KhachHang();
        k.setId(id);
        k.setHoTen("Khách " + id);
        k.setEmail(email);
        return k;
    }

    private static Map<String, Object> doiXong(MailService s, String ma) throws Exception {
        Map<String, Object> kq = s.ketQuaLo(ma);
        for (int i = 0; i < 200 && !Boolean.TRUE.equals(kq.get("xong")); i++) {
            Thread.sleep(25);
            kq = s.ketQuaLo(ma);
        }
        return kq;
    }

    @Test
    void chuaCoLoNao_xongVaTongBangKhong() {
        MailService s = new MailService(new MailServiceTest.NoiCap(null), "", "", "", "FootStyle", "");
        Map<String, Object> kq = s.ketQuaLo("VCHKHONG");
        assertEquals(0, kq.get("tong"));
        assertEquals(true, kq.get("xong"));
    }

    @Test
    void guiThanhCong_demDuSoThu() throws Exception {
        MailServiceTest.GhiThu sender = new MailServiceTest.GhiThu();
        MailService s = new MailService(new MailServiceTest.NoiCap(sender), "", "shop@gmail.com", "mk", "FootStyle", "");
        s.batDauLo("VCH1", 3);
        s.guiPhieuGiamGia(Loai.MOI, phieu("VCH1"), khach(1, "a@mail.vn"));
        s.guiPhieuGiamGia(Loai.CAP_NHAT, phieu("VCH1"), khach(2, "b@mail.vn"));
        s.guiPhieuGiamGia(Loai.HUY, phieu("VCH1"), khach(3, "c@mail.vn"));
        Map<String, Object> kq = doiXong(s, "VCH1");
        assertEquals(3, kq.get("tong"));
        assertEquals(3, kq.get("daGui"));
        assertEquals(0, kq.get("loi"));
        assertEquals(3, sender.daGui.size());
    }

    @Test
    void guiLoi_baoSoThuLoiVaLyDoThat() throws Exception {
        MailServiceTest.GhiThu sender = new MailServiceTest.GhiThu();
        sender.loi = true; // Gmail trả 535 sai mật khẩu
        MailService s = new MailService(new MailServiceTest.NoiCap(sender), "", "shop@gmail.com", "mk", "FootStyle", "");
        s.batDauLo("VCH2", 2);
        s.guiPhieuGiamGia(Loai.MOI, phieu("VCH2"), khach(1, "a@mail.vn"));
        s.guiPhieuGiamGia(Loai.MOI, phieu("VCH2"), khach(2, "b@mail.vn"));
        Map<String, Object> kq = doiXong(s, "VCH2");
        assertEquals(2, kq.get("loi"));
        assertEquals(0, kq.get("daGui"));
        assertTrue(kq.get("lyDo").toString().contains("535"), "có lý do thật từ Gmail");
        assertTrue(kq.get("lyDo").toString().contains("Mật khẩu ứng dụng"), "có gợi ý cách sửa");
    }

    @Test
    void chuaCauHinh_demLaGiaLapVaTrangThaiChuaCauHinh() throws Exception {
        MailService s = new MailService(new MailServiceTest.NoiCap(null), "", "", "", "FootStyle", "");
        s.batDauLo("VCH3", 1);
        s.guiPhieuGiamGia(Loai.MOI, phieu("VCH3"), khach(1, "a@mail.vn"));
        Map<String, Object> kq = doiXong(s, "VCH3");
        assertEquals(1, kq.get("giaLap"));
        assertEquals(0, kq.get("daGui"));
        Map<String, Object> tt = s.trangThai();
        assertEquals(false, tt.get("daCauHinh"));
        assertEquals("CHUA_CAU_HINH", tt.get("dangNhap"));
    }

    @Test
    void loMoiXoaKetQuaLanLuuTruoc() throws Exception {
        MailServiceTest.GhiThu sender = new MailServiceTest.GhiThu();
        MailService s = new MailService(new MailServiceTest.NoiCap(sender), "", "shop@gmail.com", "mk", "FootStyle", "");
        s.batDauLo("VCH4", 1);
        s.guiPhieuGiamGia(Loai.MOI, phieu("VCH4"), khach(1, "a@mail.vn"));
        doiXong(s, "VCH4");
        s.batDauLo("vch4", 2); // mã không phân biệt hoa thường
        Map<String, Object> kq = s.ketQuaLo("VCH4");
        assertEquals(2, kq.get("tong"));
        assertEquals(0, kq.get("daGui"));
        assertEquals(false, kq.get("xong"));
    }
}
