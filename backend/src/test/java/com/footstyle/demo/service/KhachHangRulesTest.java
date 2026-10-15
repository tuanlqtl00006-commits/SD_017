package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.exception.ApiException;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/** Các trường hợp hợp lệ / không hợp lệ / case khó khi nhập thông tin khách hàng và địa chỉ giao hàng. */
class KhachHangRulesTest {

    private static final LocalDate HOM_NAY = LocalDate.of(2026, 10, 15);

    private static void loi(Executable e, String chua) {
        ApiException ex = assertThrows(ApiException.class, e);
        assertTrue(ex.getMessage().contains(chua), "Thông báo \"" + ex.getMessage() + "\" phải chứa \"" + chua + "\"");
    }

    @Nested
    class HoTen {
        @Test
        void hopLeVaChuanHoaKhoangTrang() {
            assertEquals("Nguyễn Văn An", KhachHangRules.hoTen("Nguyễn Văn An", "Họ tên"));
            assertEquals("Nguyễn Văn An", KhachHangRules.hoTen("   Nguyễn    Văn \t An  ", "Họ tên"));
            assertEquals("Lê", KhachHangRules.hoTen("Lê", "Họ tên")); // đúng 2 ký tự
            assertEquals("Đặng Thị Ánh Nguyệt", KhachHangRules.hoTen("Đặng Thị Ánh Nguyệt", "Họ tên"));
        }

        @Test
        void chuTiengVietDangToHopVanHopLe() {
            // "Nguyễn" gõ kiểu tổ hợp (chữ + dấu rời, hay gặp khi copy từ Mac / Word) -> đưa về dạng dựng sẵn
            String toHop = "Nguye\u0302\u0303n Van An";
            assertEquals("Nguyễn Van An", KhachHangRules.hoTen(toHop, "Họ tên"));
        }

        @Test
        void trongHoacToanKhoangTrang() {
            loi(() -> KhachHangRules.hoTen(null, "Họ tên"), "Vui lòng nhập họ tên");
            loi(() -> KhachHangRules.hoTen("", "Họ tên"), "Vui lòng nhập họ tên");
            loi(() -> KhachHangRules.hoTen("      ", "Họ tên"), "Vui lòng nhập họ tên");
        }

        @Test
        void quaNganQuaDai() {
            loi(() -> KhachHangRules.hoTen("A", "Họ tên"), "từ 2 đến 100");
            loi(() -> KhachHangRules.hoTen("A".repeat(101), "Họ tên"), "từ 2 đến 100");
            assertEquals(100, KhachHangRules.hoTen("A".repeat(100), "Họ tên").length()); // biên trên vẫn hợp lệ
        }

        @Test
        void chuaSoHoacKyTuDacBiet() {
            loi(() -> KhachHangRules.hoTen("Nguyen123", "Họ tên"), "chỉ được chứa chữ cái");
            loi(() -> KhachHangRules.hoTen("Nguyễn @n", "Họ tên"), "chỉ được chứa chữ cái");
            loi(() -> KhachHangRules.hoTen("<script>", "Họ tên"), "chỉ được chứa chữ cái");
            loi(() -> KhachHangRules.hoTen("Nguyễn_Văn", "Họ tên"), "chỉ được chứa chữ cái");
        }

        @Test
        void nhanTrongThongBao() {
            loi(() -> KhachHangRules.hoTen("", "Tên người nhận"), "Vui lòng nhập tên người nhận");
        }
    }

    @Nested
    class SoDienThoai {
        @Test
        void hopLeCacDauSo() {
            for (String s : new String[] {"0312345678", "0512345678", "0712345678", "0812345678", "0912345678"}) {
                assertEquals(s, KhachHangRules.sdt(s, "Số điện thoại"));
            }
        }

        @Test
        void tuBoKhoangTrangDauChamGachNgang() {
            assertEquals("0901234567", KhachHangRules.sdt(" 090 123 4567 ", "Số điện thoại"));
            assertEquals("0901234567", KhachHangRules.sdt("090.123.4567", "Số điện thoại"));
            assertEquals("0901234567", KhachHangRules.sdt("090-123-4567", "Số điện thoại"));
        }

        @Test
        void doiDauSoQuocTe() {
            assertEquals("0901234567", KhachHangRules.sdt("+84901234567", "Số điện thoại"));
            assertEquals("0901234567", KhachHangRules.sdt("+84 90 123 4567", "Số điện thoại"));
            assertEquals("0901234567", KhachHangRules.sdt("84901234567", "Số điện thoại"));
        }

        @Test
        void khongHopLe() {
            loi(() -> KhachHangRules.sdt("", "Số điện thoại"), "Vui lòng nhập");
            loi(() -> KhachHangRules.sdt("   ", "Số điện thoại"), "Vui lòng nhập");
            loi(() -> KhachHangRules.sdt("0123456789", "Số điện thoại"), "không hợp lệ"); // đầu 01 không còn dùng
            loi(() -> KhachHangRules.sdt("0212345678", "Số điện thoại"), "không hợp lệ"); // đầu 02 là máy bàn
            loi(() -> KhachHangRules.sdt("0000000000", "Số điện thoại"), "không hợp lệ");
            loi(() -> KhachHangRules.sdt("090123456", "Số điện thoại"), "không hợp lệ"); // 9 số
            loi(() -> KhachHangRules.sdt("09012345678", "Số điện thoại"), "không hợp lệ"); // 11 số
            loi(() -> KhachHangRules.sdt("09o1234567", "Số điện thoại"), "không hợp lệ"); // chữ o thay số 0
            loi(() -> KhachHangRules.sdt("9012345678", "Số điện thoại"), "không hợp lệ"); // thiếu số 0 đầu
        }
    }

    @Nested
    class Email {
        @Test
        void hopLeVaLuuChuThuong() {
            assertEquals("an.nguyen@gmail.com", KhachHangRules.email("an.nguyen@gmail.com"));
            assertEquals("an@gmail.com", KhachHangRules.email("  AN@Gmail.COM  "));
            assertEquals("a_b+tag@sv.fpt.edu.vn", KhachHangRules.email("a_b+tag@sv.fpt.edu.vn"));
            assertEquals("x@my-shop.vn", KhachHangRules.email("x@my-shop.vn"));
        }

        @Test
        void batBuoc() {
            loi(() -> KhachHangRules.email(null), "Vui lòng nhập email");
            loi(() -> KhachHangRules.email("   "), "Vui lòng nhập email");
        }

        @Test
        void saiDinhDang() {
            String[] sai = {"abc", "abc@", "@gmail.com", "a@b", "a@b.c", "a@@gmail.com", "a b@gmail.com",
                "a..b@gmail.com", ".a@gmail.com", "a.@gmail.com", "a@gmail..com", "a@-gmail.com", "a@gmail.com.",
                "nguyễn@gmail.com"};
            for (String s : sai) {
                loi(() -> KhachHangRules.email(s), "không đúng định dạng");
            }
        }

        @Test
        void quaDai() {
            String dai = "a".repeat(95) + "@g.com"; // 101 ký tự
            loi(() -> KhachHangRules.email(dai), "tối đa 100");
        }
    }

    @Nested
    class NgaySinh {
        @Test
        void khongBatBuoc() {
            assertNull(KhachHangRules.ngaySinh(null, HOM_NAY));
        }

        @Test
        void bienTuoi() {
            assertEquals(LocalDate.of(2016, 10, 15), KhachHangRules.ngaySinh(LocalDate.of(2016, 10, 15), HOM_NAY)); // tròn 10 tuổi hôm nay
            loi(() -> KhachHangRules.ngaySinh(LocalDate.of(2016, 10, 16), HOM_NAY), "từ 10 tuổi"); // còn 1 ngày nữa mới đủ 10
            assertEquals(LocalDate.of(1926, 10, 15), KhachHangRules.ngaySinh(LocalDate.of(1926, 10, 15), HOM_NAY)); // đúng 100 tuổi
            assertEquals(LocalDate.of(1925, 10, 16), KhachHangRules.ngaySinh(LocalDate.of(1925, 10, 16), HOM_NAY)); // 100 tuổi 364 ngày, vẫn là 100
            loi(() -> KhachHangRules.ngaySinh(LocalDate.of(1925, 10, 15), HOM_NAY), "không quá 100"); // tròn 101 tuổi hôm nay
            loi(() -> KhachHangRules.ngaySinh(LocalDate.of(1800, 1, 1), HOM_NAY), "không quá 100");
        }

        @Test
        void tuongLaiVaHomNay() {
            loi(() -> KhachHangRules.ngaySinh(HOM_NAY.plusDays(1), HOM_NAY), "tương lai");
            loi(() -> KhachHangRules.ngaySinh(HOM_NAY, HOM_NAY), "từ 10 tuổi"); // sinh hôm nay = 0 tuổi
        }

        @Test
        void sinhNgay29Thang2() {
            // Sinh 29/02/2016: đến 28/02/2026 chưa đủ 10 tuổi, 01/03/2026 thì đủ
            loi(() -> KhachHangRules.ngaySinh(LocalDate.of(2016, 2, 29), LocalDate.of(2026, 2, 28)), "từ 10 tuổi");
            assertEquals(LocalDate.of(2016, 2, 29),
                    KhachHangRules.ngaySinh(LocalDate.of(2016, 2, 29), LocalDate.of(2026, 3, 1)));
        }
    }

    @Nested
    class GioiTinhTrangThaiAnh {
        @Test
        void gioiTinh() {
            assertEquals(1, KhachHangRules.gioiTinh(null));
            assertEquals(0, KhachHangRules.gioiTinh(0));
            assertEquals(1, KhachHangRules.gioiTinh(1));
            loi(() -> KhachHangRules.gioiTinh(2), "Giới tính");
            loi(() -> KhachHangRules.gioiTinh(-1), "Giới tính");
        }

        @Test
        void trangThai() {
            assertEquals(1, KhachHangRules.trangThai(null));
            assertEquals(0, KhachHangRules.trangThai(0));
            loi(() -> KhachHangRules.trangThai(5), "Trạng thái");
        }

        @Test
        void anhDaiDien() {
            assertNull(KhachHangRules.anhDaiDien(null));
            assertNull(KhachHangRules.anhDaiDien("  "));
            assertEquals("/api/uploads/abc123.png", KhachHangRules.anhDaiDien("/api/uploads/abc123.png"));
            assertEquals("https://cdn.vn/a.jpg", KhachHangRules.anhDaiDien("https://cdn.vn/a.jpg"));
            loi(() -> KhachHangRules.anhDaiDien("/api/uploads/../application.properties"), "Ảnh đại diện");
            loi(() -> KhachHangRules.anhDaiDien("javascript:alert(1)"), "Ảnh đại diện");
            loi(() -> KhachHangRules.anhDaiDien("C:\\anh.png"), "Ảnh đại diện");
        }
    }

    @Nested
    class DiaChi {
        @Test
        void tinhPhuongBatBuoc() {
            assertEquals("Thành phố Hà Nội", KhachHangRules.tinhThanh("  Thành phố   Hà Nội "));
            loi(() -> KhachHangRules.tinhThanh(""), "Vui lòng chọn Tỉnh/Thành phố");
            loi(() -> KhachHangRules.phuongXa(null), "Vui lòng chọn Phường/Xã");
        }

        @Test
        void diaChiCuThe() {
            assertEquals("Số 12 ngõ 5 Cầu Giấy", KhachHangRules.diaChiCuThe("  Số 12   ngõ 5 Cầu Giấy "));
            assertEquals("12/3A", KhachHangRules.diaChiCuThe("12/3A")); // đúng 5 ký tự
            loi(() -> KhachHangRules.diaChiCuThe("     "), "Vui lòng nhập địa chỉ cụ thể");
            loi(() -> KhachHangRules.diaChiCuThe("aaa"), "từ 5 đến 255");
            loi(() -> KhachHangRules.diaChiCuThe("x".repeat(256)), "từ 5 đến 255");
            loi(() -> KhachHangRules.diaChiCuThe("..., ---"), "phải có chữ hoặc số");
        }
    }
}
