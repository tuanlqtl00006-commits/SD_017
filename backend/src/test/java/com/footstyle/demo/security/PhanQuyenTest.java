package com.footstyle.demo.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PhanQuyenTest {

    static final NguoiDung QL = new NguoiDung("NV", 1, "NV01", "Nguyễn Hoàng Long", NguoiDung.QUAN_LY, null);
    static final NguoiDung NV = new NguoiDung("NV", 3, "NV03", "Lê Quốc Bảo", NguoiDung.NHAN_VIEN, null);
    static final NguoiDung KH = new NguoiDung("KH", 5, "KH05", "Hoàng Đức Anh", NguoiDung.KHACH_HANG, null);

    @Test
    void duongDanCongKhai() {
        assertTrue(PhanQuyen.congKhai("POST", "/api/auth/dang-nhap"));
        assertTrue(PhanQuyen.congKhai("POST", "/api/auth/khach-hang/dang-nhap"));
        assertTrue(PhanQuyen.congKhai("POST", "/api/auth/quen-mat-khau"));
        assertTrue(PhanQuyen.congKhai("GET", "/api/auth/cau-hinh"));
        assertTrue(PhanQuyen.congKhai("GET", "/api/uploads/anh.png"));
        assertTrue(PhanQuyen.congKhai("POST", "/api/ho-tro/khach/bat-dau"));
        assertTrue(PhanQuyen.congKhai("GET", "/api/ho-tro/khach/12"));
        assertTrue(PhanQuyen.congKhai("POST", "/api/ho-tro/khach/12/tin-nhan"));
        assertTrue(PhanQuyen.congKhai("GET", "/index.html"), "ngoài /api không thuộc phạm vi kiểm tra");
        // không công khai
        assertFalse(PhanQuyen.congKhai("GET", "/api/auth/dang-nhap"), "chỉ POST mới công khai");
        assertFalse(PhanQuyen.congKhai("GET", "/api/auth/toi"));
        assertFalse(PhanQuyen.congKhai("PUT", "/api/auth/doi-mat-khau"));
        assertFalse(PhanQuyen.congKhai("GET", "/api/hoa-don"));
        assertFalse(PhanQuyen.congKhai("GET", "/api/ho-tro"), "trang hỗ trợ của nhân viên phải đăng nhập");
        assertFalse(PhanQuyen.congKhai("POST", "/api/ho-tro/12/tin-nhan"));
        assertFalse(PhanQuyen.congKhai("GET", "/api/ho-tro/khachx"), "không nhầm tiền tố");
        assertFalse(PhanQuyen.congKhai("GET", "/api/chat/chua-doc"));
        assertFalse(PhanQuyen.congKhai("GET", "/api/uploadsx/a.png"), "không nhầm tiền tố");
    }

    @Test
    void quanLy_duocMoiThu() {
        for (String p : new String[] {"/api/nhan-vien", "/api/thong-ke", "/api/lich-lam-viec", "/api/san-pham/1", "/api/khach-hang/2"}) {
            assertTrue(PhanQuyen.duocPhep(QL, "DELETE", p), p);
            assertTrue(PhanQuyen.duocPhep(QL, "POST", p), p);
        }
    }

    @Test
    void nhanVien_banHangHoaDonKhachHangGiaoCaChat() {
        assertTrue(PhanQuyen.duocPhep(NV, "POST", "/api/ban-hang/thanh-toan"));
        assertTrue(PhanQuyen.duocPhep(NV, "PUT", "/api/hoa-don/5/trang-thai"));
        assertTrue(PhanQuyen.duocPhep(NV, "POST", "/api/khach-hang"));
        assertTrue(PhanQuyen.duocPhep(NV, "PUT", "/api/khach-hang/2"));
        assertTrue(PhanQuyen.duocPhep(NV, "POST", "/api/dia-chi/khach-hang/2"));
        assertTrue(PhanQuyen.duocPhep(NV, "POST", "/api/giao-ca/nhan-ca"));
        assertTrue(PhanQuyen.duocPhep(NV, "POST", "/api/ho-tro/3/tin-nhan"));
        assertTrue(PhanQuyen.duocPhep(NV, "GET", "/api/chat/chua-doc"));
        assertTrue(PhanQuyen.duocPhep(NV, "POST", "/api/chat/noi-bo/rieng"));
        assertTrue(PhanQuyen.duocPhep(NV, "GET", "/api/lich-lam-viec/cua-toi"));
        assertTrue(PhanQuyen.duocPhep(NV, "PUT", "/api/auth/doi-mat-khau"));
        assertTrue(PhanQuyen.duocPhep(NV, "POST", "/api/hinh-anh/upload"));
        assertTrue(PhanQuyen.duocPhep(NV, "POST", "/api/tro-ly/hoi"));
        assertFalse(PhanQuyen.congKhai("POST", "/api/tro-ly/hoi"), "trợ lý nội bộ phải đăng nhập");
        assertFalse(PhanQuyen.duocPhep(NV, "POST", "/api/lich-lam-viec/import"), "nhập Excel lịch chỉ Quản lý");
    }

    @Test
    void nhanVien_chiXemSanPhamGiamGia() {
        for (String p : new String[] {"/api/san-pham", "/api/bien-the-san-pham", "/api/thuoc-tinh/mau-sac", "/api/phieu-giam-gia",
                "/api/dot-giam-gia", "/api/giam-gia/ap-dung", "/api/ca-lam-viec"}) {
            assertTrue(PhanQuyen.duocPhep(NV, "GET", p), p);
            assertFalse(PhanQuyen.duocPhep(NV, "POST", p), p);
            assertFalse(PhanQuyen.duocPhep(NV, "PUT", p + "/1"), p);
            assertFalse(PhanQuyen.duocPhep(NV, "DELETE", p + "/1"), p);
        }
    }

    @Test
    void nhanVien_khongVaoChucNangQuanLy() {
        assertFalse(PhanQuyen.duocPhep(NV, "GET", "/api/nhan-vien"));
        assertFalse(PhanQuyen.duocPhep(NV, "GET", "/api/nhan-vien/3"));
        assertFalse(PhanQuyen.duocPhep(NV, "GET", "/api/thong-ke"), "thống kê chỉ quản lý xem");
        assertFalse(PhanQuyen.duocPhep(NV, "GET", "/api/thong-ke/tong-quan"));
        assertFalse(PhanQuyen.duocPhep(NV, "GET", "/api/lich-lam-viec"), "xem lịch của mọi người là của quản lý");
        assertFalse(PhanQuyen.duocPhep(NV, "POST", "/api/lich-lam-viec"));
        assertFalse(PhanQuyen.duocPhep(NV, "DELETE", "/api/khach-hang/2"), "xóa khách hàng chỉ quản lý");
        assertFalse(PhanQuyen.duocPhep(NV, "GET", "/api/hoa-donxyz"), "không nhầm tiền tố");
    }

    @Test
    void khongPhanBietHoaThuong_vaDauGachCuoi() {
        assertFalse(PhanQuyen.duocPhep(NV, "get", "/API/NHAN-VIEN/"));
        assertTrue(PhanQuyen.duocPhep(NV, "get", "/api/hoa-don/"));
        assertTrue(PhanQuyen.duocPhep(NV, "GET", "/api/hoa-don?page=0"));
    }

    @Test
    void khachHang_chiApiTaiKhoan() {
        assertTrue(PhanQuyen.duocPhep(KH, "GET", "/api/auth/toi"));
        assertTrue(PhanQuyen.duocPhep(KH, "GET", "/api/khach/don-hang"));
        assertFalse(PhanQuyen.duocPhep(KH, "GET", "/api/hoa-don"));
        assertFalse(PhanQuyen.duocPhep(KH, "GET", "/api/khach-hang"), "khách không xem danh sách khách hàng");
        assertFalse(PhanQuyen.duocPhep(KH, "POST", "/api/ban-hang/thanh-toan"));
        assertFalse(PhanQuyen.duocPhep(null, "GET", "/api/hoa-don"));
    }
}
