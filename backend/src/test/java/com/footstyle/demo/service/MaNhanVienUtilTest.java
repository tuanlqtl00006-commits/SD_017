package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Mã nhân viên tạo theo họ tên đầy đủ: Tên + chữ cái đầu của họ và tên đệm + số thứ tự 2 chữ số. */
class MaNhanVienUtilTest {

    @Test
    void tienToLayTenCongChuCaiDauHoVaDem() {
        assertEquals("AnNV", MaNhanVienUtil.tienTo("Nguyễn Văn An"));
        assertEquals("LanTTM", MaNhanVienUtil.tienTo("trần thị mai lan"));
        assertEquals("DucDV", MaNhanVienUtil.tienTo("Đặng Văn Đức"));
        assertEquals("Lan", MaNhanVienUtil.tienTo("Lan"));
        assertEquals("BaoLQ", MaNhanVienUtil.tienTo("  Lê   Quốc  Bảo  "));
        assertEquals("TrangDT", MaNhanVienUtil.tienTo("ĐỖ THÙY TRANG"));
        assertEquals("", MaNhanVienUtil.tienTo("   "));
    }

    @Test
    void danhSoThuTuKhongTrung() {
        assertEquals("AnNV01", MaNhanVienUtil.taoMa("Nguyễn Văn An", List.of("NV0001", "NV0002")));
        assertEquals("AnNV03", MaNhanVienUtil.taoMa("Ngô Văn An", List.of("AnNV01", "AnNV02", "NV0001")));
        assertEquals("AnNV03", MaNhanVienUtil.taoMa("Nguyễn Văn An", List.of("annv02")));
        assertEquals("AnNV01", MaNhanVienUtil.taoMa("Nguyễn Văn An", List.of("AnNVX05")));
        assertEquals("AnNV01", MaNhanVienUtil.taoMa("Nguyễn Văn An", null));
        assertEquals("NV01", MaNhanVienUtil.taoMa("123", List.of()));
    }
}
