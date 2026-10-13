package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Mã nhân viên: chữ cái đầu của mọi từ trong họ tên (không dấu) + số thứ tự tối thiểu 2 chữ số. Ví dụ Lê Quốc Tuấn -> LQT01. */
class MaNhanVienUtilTest {

    @Test
    void tienToLaChuCaiDauCuaMoiTu() {
        assertEquals("LQT", MaNhanVienUtil.tienTo("Lê Quốc Tuấn"));
        assertEquals("LQT", MaNhanVienUtil.tienTo("  lê   quốc  tuấn "));
        assertEquals("NVA", MaNhanVienUtil.tienTo("Nguyễn Văn An"));
        assertEquals("TTML", MaNhanVienUtil.tienTo("trần thị mai lan"));
        assertEquals("DVD", MaNhanVienUtil.tienTo("Đặng Văn Đức"));
        assertEquals("DTT", MaNhanVienUtil.tienTo("ĐỖ THÙY TRANG"));
        assertEquals("L", MaNhanVienUtil.tienTo("Lan"));
        assertEquals("", MaNhanVienUtil.tienTo("   "));
        assertEquals("", MaNhanVienUtil.tienTo("123 ???"));
        assertEquals("", MaNhanVienUtil.tienTo(null));
    }

    @Test
    void nguoiDauTienLaSo01() {
        assertEquals("LQT01", MaNhanVienUtil.taoMa("Lê Quốc Tuấn", List.of("NV01", "NV02")));
        assertEquals("LQT01", MaNhanVienUtil.taoMa("Lê Quốc Tuấn", null));
        assertEquals("LQT01", MaNhanVienUtil.taoMa("Lê Quốc Tuấn", List.of()));
        assertEquals("NVA01", MaNhanVienUtil.taoMa("Nguyễn Văn An", List.of()));
        assertEquals("TTML01", MaNhanVienUtil.taoMa("Trần Thị Mai Lan", List.of()));
        assertEquals("L01", MaNhanVienUtil.taoMa("Lan", List.of()));
    }

    @Test
    void nguoiTrungChuCaiDauDanhSoTiep() {
        assertEquals("LQT02", MaNhanVienUtil.taoMa("Lê Quang Tú", List.of("LQT01", "NV01")));
        assertEquals("LQT04", MaNhanVienUtil.taoMa("Lê Quốc Tuấn", List.of("LQT01", "LQT03")));
        assertEquals("LQT03", MaNhanVienUtil.taoMa("Lê Quốc Tuấn", List.of("lqt02"))); // không phân biệt hoa thường
    }

    @Test
    void docDuocCaMaCuNhieuSo0() {
        // CSDL chưa rút gọn (LQT0001, NV0010) vẫn tính đúng số lớn nhất, mã mới luôn ở dạng gọn
        assertEquals("LQT02", MaNhanVienUtil.taoMa("Lê Quốc Tuấn", List.of("LQT0001")));
        assertEquals("NV11", MaNhanVienUtil.taoMa("Ngô Vinh", List.of("NV0001", "NV0010")));
    }

    @Test
    void khongNhamVoiMaCoTienToDaiHonHoacMaKhongPhaiSo() {
        assertEquals("LQT01", MaNhanVienUtil.taoMa("Lê Quốc Tuấn", List.of("LQTA01"))); // tiền tố khác
        assertEquals("LQT01", MaNhanVienUtil.taoMa("Lê Quốc Tuấn", List.of("LQTX05", "LQT"))); // phần đuôi không phải số
    }

    @Test
    void maMauNv01GiuNguyenVaNguoiCoTienToNvDanhSoTiep() {
        // Mã mẫu NV01..NV10 không bị đổi; người có chữ cái đầu là N-V vẫn không trùng mã mẫu
        List<String> mau = List.of("NV01", "NV02", "NV03", "NV10");
        assertEquals("NV11", MaNhanVienUtil.taoMa("Ngô Vinh", mau));
    }

    @Test
    void tenKhongCoChuCaiDungTienToDuPhongNv() {
        assertEquals("NV01", MaNhanVienUtil.taoMa("123", List.of()));
        assertEquals("NV11", MaNhanVienUtil.taoMa(null, List.of("NV10")));
    }

    @Test
    void duoi10KhongCan0ThuaVaTren99VietThangKhongDemSo0() {
        assertEquals("LQT09", MaNhanVienUtil.taoMa("Lê Quốc Tuấn", List.of("LQT08")));
        assertEquals("LQT10", MaNhanVienUtil.taoMa("Lê Quốc Tuấn", List.of("LQT09")));
        assertEquals("LQT100", MaNhanVienUtil.taoMa("Lê Quốc Tuấn", List.of("LQT99")));
        assertEquals("LQT1000", MaNhanVienUtil.taoMa("Lê Quốc Tuấn", List.of("LQT999")));
    }
}
