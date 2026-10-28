package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.dto.HoaDonTrangThaiRequest;
import com.footstyle.demo.entity.HoaDon;
import com.footstyle.demo.entity.KhachHang;
import com.footstyle.demo.entity.LichSuThanhToan;
import com.footstyle.demo.entity.PhieuGiamGia;
import com.footstyle.demo.entity.PhieuGiamGiaKhachHang;
import com.footstyle.demo.exception.ApiException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Hủy hóa đơn có dùng phiếu giảm giá thì trả lại lượt phiếu (cô dễ hỏi "hủy đơn thì voucher sao?"):
 * lượt đã dùng giảm 1 (không âm), phiếu cá nhân thì khách dùng lại được, ghi vào lịch sử hóa đơn, mỗi hóa đơn chỉ hoàn một lần.
 */
class HoaDonHuyHoanPhieuTest {

    private final HoaDonKhoGia kho = new HoaDonKhoGia();
    private final HoaDon hd = kho.hd;
    private final HoaDonService service = kho.service;

    private static HoaDonTrangThaiRequest huy() {
        return new HoaDonTrangThaiRequest(HoaDon.DA_HUY, "Khách đổi ý", null);
    }

    private static PhieuGiamGia phieu(int doiTuong, int daDung) {
        PhieuGiamGia p = new PhieuGiamGia();
        p.setId(7);
        p.setMaPhieu("GIAM50K");
        p.setDoiTuongApDung(doiTuong);
        p.setSoLuong(10);
        p.setSoLuongDaDung(daDung);
        return p;
    }

    private KhachHang khach() {
        KhachHang kh = new KhachHang();
        kh.setId(5);
        kh.setHoTen("Nguyễn Thị Lan");
        hd.setKhachHang(kh);
        return kh;
    }

    private PhieuGiamGiaKhachHang tangPhieuCaNhan(LocalDateTime ngaySuDung) {
        PhieuGiamGiaKhachHang x = new PhieuGiamGiaKhachHang();
        x.setId(1);
        x.setIdKhachHang(5);
        x.setIdPhieuGiamGia(7);
        x.setNgaySuDung(ngaySuDung);
        x.setTrangThai(1);
        kho.phieuKhach.add(x);
        return x;
    }

    private String moTaCuoi() {
        return kho.lichSu.get(kho.lichSu.size() - 1).getMoTa();
    }

    @Test
    void huyDonOnline_dungPhieuChung_traLaiMotLuot_vaGhiLichSu() {
        PhieuGiamGia p = phieu(PhieuGiamGia.DOI_TUONG_TAT_CA, 3);
        hd.setPhieuGiamGia(p);
        service.doiTrangThai(1, huy());
        assertEquals(HoaDon.DA_HUY, hd.getTrangThai());
        assertEquals(2, p.getSoLuongDaDung());
        assertEquals("Khách đổi ý - Trả lại lượt phiếu GIAM50K", moTaCuoi());
    }

    @Test
    void huyDon_phieuCaNhan_khachDungLaiDuoc() {
        khach();
        PhieuGiamGia p = phieu(PhieuGiamGia.DOI_TUONG_KHACH_CU_THE, 1);
        hd.setPhieuGiamGia(p);
        PhieuGiamGiaKhachHang cuaKhach = tangPhieuCaNhan(LocalDateTime.of(2026, 10, 20, 9, 0));
        service.doiTrangThai(1, huy());
        assertEquals(0, p.getSoLuongDaDung());
        assertNull(cuaKhach.getNgaySuDung(), "xóa ngày sử dụng thì phiếu lại hiện trong danh sách phiếu của khách");
        assertTrue(moTaCuoi().contains("Trả lại lượt phiếu GIAM50K"));
    }

    @Test
    void huyDon_phieuCaNhan_khachConDonKhacChuaHuyDungPhieuNay_thiGiuNgaySuDung() {
        khach();
        hd.setPhieuGiamGia(phieu(PhieuGiamGia.DOI_TUONG_KHACH_CU_THE, 2));
        LocalDateTime ngay = LocalDateTime.of(2026, 10, 20, 9, 0);
        PhieuGiamGiaKhachHang cuaKhach = tangPhieuCaNhan(ngay);
        kho.donKhacDungPhieu = 1;
        service.doiTrangThai(1, huy());
        assertEquals(1, hd.getPhieuGiamGia().getSoLuongDaDung(), "lượt của đơn bị hủy vẫn được trả lại");
        assertEquals(ngay, cuaKhach.getNgaySuDung(), "đơn kia vẫn đang dùng phiếu nên khách chưa dùng lại được");
    }

    @Test
    void huyDon_phieuCaNhanCuaKhachKhac_khongDungToi() {
        khach();
        hd.setPhieuGiamGia(phieu(PhieuGiamGia.DOI_TUONG_KHACH_CU_THE, 1));
        PhieuGiamGiaKhachHang cuaNguoiKhac = tangPhieuCaNhan(LocalDateTime.of(2026, 10, 20, 9, 0));
        cuaNguoiKhac.setIdKhachHang(9);
        service.doiTrangThai(1, huy());
        assertNotNull(cuaNguoiKhac.getNgaySuDung());
    }

    @Test
    void huyDon_luotDaDungBang0_khongAm_khongGhiTraLai() {
        PhieuGiamGia p = phieu(PhieuGiamGia.DOI_TUONG_TAT_CA, 0);
        hd.setPhieuGiamGia(p);
        service.doiTrangThai(1, huy());
        assertEquals(0, p.getSoLuongDaDung());
        assertEquals("Khách đổi ý", moTaCuoi(), "không giảm được thì không ghi là đã trả lại lượt");
    }

    @Test
    void huyDon_khongDungPhieu_lichSuNhuCu() {
        service.doiTrangThai(1, huy());
        assertEquals(HoaDon.DA_HUY, hd.getTrangThai());
        assertEquals("Khách đổi ý", moTaCuoi());
    }

    @Test
    void donGiaoTaiQuay_daThanhToan_giaoThatBaiRoiHuy_traLuotPhieuVaHoanPhi() {
        hd.setTrangThai(HoaDon.GIAO_THAT_BAI);
        PhieuGiamGia p = phieu(PhieuGiamGia.DOI_TUONG_TAT_CA, 4);
        hd.setPhieuGiamGia(p);
        LichSuThanhToan tt = new LichSuThanhToan();
        tt.setHoaDon(hd);
        tt.setSoTien(new BigDecimal("450000"));
        tt.setTrangThai(LichSuThanhToan.DA_THANH_TOAN);
        tt.setThoiGian(LocalDateTime.of(2026, 10, 20, 9, 0));
        kho.thanhToan.add(tt);
        service.doiTrangThai(1, huy());
        assertEquals(3, p.getSoLuongDaDung());
        assertTrue(moTaCuoi().contains("Hoàn phí cho khách"));
        assertTrue(moTaCuoi().endsWith("Trả lại lượt phiếu GIAM50K"));
    }

    @Test
    void doiSangTrangThaiKhac_khongDungToiPhieu() {
        PhieuGiamGia p = phieu(PhieuGiamGia.DOI_TUONG_TAT_CA, 3);
        hd.setPhieuGiamGia(p);
        service.doiTrangThai(1, new HoaDonTrangThaiRequest(HoaDon.DA_XAC_NHAN, null, null));
        assertEquals(3, p.getSoLuongDaDung());
        assertFalse(moTaCuoi().contains("phiếu"));
    }

    @Test
    void huyLaiDonDaHuy_biChan_luotChiHoanMotLan() {
        PhieuGiamGia p = phieu(PhieuGiamGia.DOI_TUONG_TAT_CA, 3);
        hd.setPhieuGiamGia(p);
        service.doiTrangThai(1, huy());
        assertThrows(ApiException.class, () -> service.doiTrangThai(1, huy()));
        assertEquals(2, p.getSoLuongDaDung());
    }
}
