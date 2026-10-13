package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** Quy tắc chung của mọi mã tự cấp: tiền tố + số thứ tự tối thiểu 2 chữ số (HD01, HD99, HD100), không đệm thêm số 0. */
class MaUtilTest {

    @Test
    void dinhDangToiThieu2ChuSoKhongDemThem() {
        assertEquals("HD01", MaUtil.dinhDang("HD", 1));
        assertEquals("HD09", MaUtil.dinhDang("HD", 9));
        assertEquals("HD10", MaUtil.dinhDang("HD", 10));
        assertEquals("HD99", MaUtil.dinhDang("HD", 99));
        assertEquals("HD100", MaUtil.dinhDang("HD", 100)); // lên 100 thì HD100, không bị đổi dạng
        assertEquals("HD1000", MaUtil.dinhDang("HD", 1000));
        assertEquals("DGG01", MaUtil.dinhDang("DGG", 1));
        assertEquals("01", MaUtil.dinhDang(null, 1));
    }

    @Test
    void rutGonMaCuNhieuSo0() {
        assertEquals("HD01", MaUtil.rutGon("HD000001"));
        assertEquals("HD18", MaUtil.rutGon("HD000018"));
        assertEquals("NV01", MaUtil.rutGon("NV0001"));
        assertEquals("NV10", MaUtil.rutGon("NV0010"));
        assertEquals("KH06", MaUtil.rutGon("KH0006"));
        assertEquals("SP01", MaUtil.rutGon("SP001"));
        assertEquals("DGG09", MaUtil.rutGon("DGG009"));
        assertEquals("DM01", MaUtil.rutGon("DM001"));
        assertEquals("DC00", MaUtil.rutGon("DC000")); // mục "Không áp dụng"
        assertEquals("LQT01", MaUtil.rutGon("LQT0001"));
        assertEquals("HD100", MaUtil.rutGon("HD000100"));
        assertEquals("HD1000", MaUtil.rutGon("HD001000"));
    }

    @Test
    void maDaGonHoacKhongPhaiDangChuCongSoThiGiuNguyen() {
        assertEquals("HD01", MaUtil.rutGon("HD01"));
        assertEquals("HD100", MaUtil.rutGon("HD100"));
        assertEquals("HD01", MaUtil.rutGon(MaUtil.rutGon("HD000001"))); // chạy lại không đổi thêm
        assertEquals("VCH7QX2YN", MaUtil.rutGon("VCH7QX2YN")); // mã phiếu giảm giá nhập tay
        assertEquals("VCHDOI100", MaUtil.rutGon("VCHDOI100"));
        assertEquals("SP001-DEN-4U-G5", MaUtil.rutGon("SP001-DEN-4U-G5")); // mã biến thể có hàm riêng
        assertEquals("", MaUtil.rutGon(""));
        assertNull(MaUtil.rutGon(null));
    }

    @Test
    void rutGonMaBienTheChiDoiPhanMaSanPham() {
        assertEquals("SP01-DEN-4U-G5", MaUtil.rutGonMaBienThe("SP001-DEN-4U-G5"));
        assertEquals("SP06-TRG-4U-G5", MaUtil.rutGonMaBienThe("SP006-TRG-4U-G5"));
        assertEquals("SP04-DEN-40", MaUtil.rutGonMaBienThe("SP004-DEN-40"));
        assertEquals("SP12-DEN-4U-G5-2", MaUtil.rutGonMaBienThe("SP012-DEN-4U-G5-2"));
        assertEquals("SP01-DEN-4U-G5", MaUtil.rutGonMaBienThe("SP01-DEN-4U-G5")); // đã gọn
        assertEquals("SP01", MaUtil.rutGonMaBienThe("SP001")); // không có gạch ngang
        assertNull(MaUtil.rutGonMaBienThe(null));
    }
}
