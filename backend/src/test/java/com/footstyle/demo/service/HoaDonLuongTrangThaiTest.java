package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.HoaDon;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Bảng luật chuyển trạng thái hóa đơn (nguồn duy nhất cho cả kiểm tra ở backend lẫn nút hiện trên giao diện). */
class HoaDonLuongTrangThaiTest {

    private static List<Integer> giaoHang(int hienTai) {
        return HoaDonLuongTrangThai.chuyenDuoc(HoaDon.GIAO_HANG, hienTai);
    }

    private static List<Integer> taiQuay(int hienTai) {
        return HoaDonLuongTrangThai.chuyenDuoc(HoaDon.TAI_QUAY, hienTai);
    }

    @Test
    void donGiaoHang_luongChinhDiTuongBuoc_vaHuyDuocKhiChuaGiaoChoVanChuyen() {
        assertEquals(List.of(HoaDon.DA_XAC_NHAN, HoaDon.DA_HUY), giaoHang(HoaDon.CHO_XAC_NHAN));
        assertEquals(List.of(HoaDon.CHO_LAY_HANG, HoaDon.DA_HUY), giaoHang(HoaDon.DA_XAC_NHAN));
        assertEquals(List.of(HoaDon.DANG_GIAO, HoaDon.DA_HUY), giaoHang(HoaDon.CHO_LAY_HANG));
        assertEquals(List.of(HoaDon.HOAN_THANH), giaoHang(HoaDon.DA_GIAO), "đã giao thì chỉ còn hoàn thành, hết hủy");
    }

    @Test
    void dangGiaoHang_baoDuocGiaoThatBai_nhungKhongHuyThang() {
        assertEquals(List.of(HoaDon.DA_GIAO, HoaDon.GIAO_THAT_BAI), giaoHang(HoaDon.DANG_GIAO));
        assertFalse(giaoHang(HoaDon.DANG_GIAO).contains(HoaDon.DA_HUY), "hàng đang ở chỗ vận chuyển nên chưa hủy trực tiếp được");
    }

    @Test
    void giaoHangThatBai_giaoLaiHoacHuy_khongNhayCoc() {
        assertEquals(List.of(HoaDon.DANG_GIAO, HoaDon.DA_HUY), giaoHang(HoaDon.GIAO_THAT_BAI));
        assertFalse(giaoHang(HoaDon.GIAO_THAT_BAI).contains(HoaDon.DA_GIAO));
        assertFalse(giaoHang(HoaDon.GIAO_THAT_BAI).contains(HoaDon.HOAN_THANH));
    }

    @Test
    void donTaiQuay_chiHoanThanhHoacHuy_khongCoGiaoThatBai() {
        assertEquals(List.of(HoaDon.HOAN_THANH, HoaDon.DA_HUY), taiQuay(HoaDon.CHO_XAC_NHAN));
        assertEquals(List.of(), taiQuay(HoaDon.GIAO_THAT_BAI), "đơn tại quầy không thể rơi vào trạng thái giao hàng thất bại");
        assertFalse(taiQuay(HoaDon.CHO_XAC_NHAN).contains(HoaDon.GIAO_THAT_BAI));
        // loại đơn chưa ghi coi như tại quầy
        assertEquals(List.of(HoaDon.HOAN_THANH, HoaDon.DA_HUY), HoaDonLuongTrangThai.chuyenDuoc(null, HoaDon.CHO_XAC_NHAN));
    }

    @Test
    void trangThaiCuoi_khongConDuongNaoDiTiep() {
        for (int cuoi : new int[] {HoaDon.DA_HUY, HoaDon.HOAN_THANH, HoaDon.HOAN_TIEN}) {
            assertTrue(HoaDonLuongTrangThai.laTrangThaiCuoi(cuoi), "trạng thái " + cuoi);
            assertEquals(List.of(), giaoHang(cuoi), "giao hàng, trạng thái " + cuoi);
            assertEquals(List.of(), taiQuay(cuoi), "tại quầy, trạng thái " + cuoi);
        }
        for (int dangXuLy : new int[] {1, 2, 3, 4, 5, HoaDon.GIAO_THAT_BAI}) {
            assertFalse(HoaDonLuongTrangThai.laTrangThaiCuoi(dangXuLy), "trạng thái " + dangXuLy);
            assertFalse(giaoHang(dangXuLy).isEmpty(), "đơn giao hàng ở " + dangXuLy + " còn bước tiếp");
        }
    }

    @Test
    void chiHuyVaGiaoThatBaiMoiBatBuocLyDo() {
        for (int tt = 0; tt <= HoaDon.GIAO_THAT_BAI; tt++) {
            boolean can = tt == HoaDon.DA_HUY || tt == HoaDon.GIAO_THAT_BAI;
            assertEquals(can, HoaDonLuongTrangThai.batBuocLyDo(tt), "trạng thái " + tt);
        }
    }

    @Test
    void moiBuocDuocPhepDeuLaTrangThaiCoTen_vaTenDungChinhTa() {
        for (int loai : new int[] {HoaDon.TAI_QUAY, HoaDon.GIAO_HANG}) {
            for (int tt = 0; tt <= HoaDon.GIAO_THAT_BAI; tt++) {
                for (int sang : HoaDonLuongTrangThai.chuyenDuoc(loai, tt)) {
                    assertTrue(HoaDonLuongTrangThai.hopLe(sang), tt + " -> " + sang);
                    assertTrue(sang != tt, "không chuyển về chính nó: " + tt);
                }
            }
        }
        assertEquals("Giao hàng thất bại", HoaDonLuongTrangThai.ten(HoaDon.GIAO_THAT_BAI));
        assertEquals("Đã hoàn thành", HoaDonLuongTrangThai.ten(HoaDon.HOAN_THANH));
        assertEquals("Hoàn tiền", HoaDonLuongTrangThai.ten(HoaDon.HOAN_TIEN));
        assertEquals("Không xác định", HoaDonLuongTrangThai.ten(99));
        assertEquals("Không xác định", HoaDonLuongTrangThai.ten(null));
        assertFalse(HoaDonLuongTrangThai.hopLe(9));
        assertFalse(HoaDonLuongTrangThai.hopLe(-1));
        assertFalse(HoaDonLuongTrangThai.hopLe(null));
    }

    @Test
    void bangTenCoDungChinTrangThaiTu0Den8() {
        assertNull(HoaDonLuongTrangThai.tatCaTen().get(99));
        assertEquals(9, HoaDonLuongTrangThai.tatCaTen().size(), "0..8");
        for (int tt = 0; tt <= HoaDon.GIAO_THAT_BAI; tt++) {
            assertTrue(HoaDonLuongTrangThai.tatCaTen().containsKey(tt), "thiếu trạng thái " + tt);
        }
    }
}
