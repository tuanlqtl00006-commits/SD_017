package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.service.PhieuMailKeHoach.KeHoach;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Kiểm tra PhieuMailKeHoach theo đúng ví dụ trong ghi chú: phiếu cá nhân tặng khách KH2, KH4, KH5 rồi sửa.
 * Khách ở đây được ghi bằng id (2, 4, 5 ...).
 */
class PhieuMailKeHoachTest {

    private static final List<Integer> KHACH_CU = List.of(2, 4, 5);

    private static void khop(KeHoach kh, List<Integer> moi, List<Integer> huy, List<Integer> capNhat) {
        assertEquals(moi, kh.moi(), "mail MỚI");
        assertEquals(huy, kh.huy(), "mail HỦY");
        assertEquals(capNhat, kh.capNhat(), "mail CẬP NHẬT");
        assertEquals(moi.size() + huy.size() + capNhat.size(), kh.tong());
    }

    @Test
    void th1_doiThongTinPhieuDanhSachKhachGiuNguyen_moiKhachNhanMailCapNhat() {
        khop(PhieuMailKeHoach.lap(KHACH_CU, List.of(2, 4, 5), true), List.of(), List.of(), List.of(2, 4, 5));
    }

    @Test
    void th1_thuTuKhachTrongDanhSachMoiKhongLamDoiKetQua() {
        KeHoach kh = PhieuMailKeHoach.lap(KHACH_CU, List.of(5, 2, 4), true);
        assertEquals(Set.of(2, 4, 5), new HashSet<>(kh.capNhat()));
        assertTrue(kh.moi().isEmpty());
        assertTrue(kh.huy().isEmpty());
    }

    @Test
    void th2_thongTinKhongDoiDoiKhachThanh_1_3_5() {
        // 5 giữ nguyên -> không gửi; 1 và 3 -> mail mới; 2 và 4 bị bỏ -> mail hủy
        khop(PhieuMailKeHoach.lap(KHACH_CU, List.of(1, 3, 5), false), List.of(1, 3), List.of(2, 4), List.of());
    }

    @Test
    void th3_doiCaThongTinVaKhach() {
        // 5 -> cập nhật lại thông tin phiếu; 2, 4 -> hủy; 1, 3 -> mới
        khop(PhieuMailKeHoach.lap(KHACH_CU, List.of(1, 3, 5), true), List.of(1, 3), List.of(2, 4), List.of(5));
    }

    @Test
    void khongDoiGiCaKhongGuiMailNao() {
        KeHoach kh = PhieuMailKeHoach.lap(KHACH_CU, List.of(2, 4, 5), false);
        khop(kh, List.of(), List.of(), List.of());
        assertEquals(0, kh.tong());
    }

    @Test
    void doiTuCaNhanSangCongKhai_danhSachMoiRong_moiKhachCuNhanMailHuy() {
        // Dù thông tin phiếu có đổi hay không, khách cũ đều bị thu hồi phiếu
        khop(PhieuMailKeHoach.lap(KHACH_CU, List.of(), false), List.of(), List.of(2, 4, 5), List.of());
        khop(PhieuMailKeHoach.lap(KHACH_CU, List.of(), true), List.of(), List.of(2, 4, 5), List.of());
    }

    @Test
    void doiTuCongKhaiSangCaNhan_moiKhachDuocTangNhanMailMoi() {
        khop(PhieuMailKeHoach.lap(List.of(), List.of(1, 3), false), List.of(1, 3), List.of(), List.of());
        khop(PhieuMailKeHoach.lap(List.of(), List.of(1, 3), true), List.of(1, 3), List.of(), List.of());
    }

    @Test
    void congKhaiGiuNguyen_khongCoKhachNaoThiKhongGuiGi() {
        khop(PhieuMailKeHoach.lap(List.of(), List.of(), true), List.of(), List.of(), List.of());
    }

    @Test
    void idTrungTrongDanhSachKhongGuiHaiLan() {
        KeHoach kh = PhieuMailKeHoach.lap(List.of(2, 2, 4, 5, 5), List.of(1, 1, 3, 5, 5, 3), true);
        khop(kh, List.of(1, 3), List.of(2, 4), List.of(5));

        List<Integer> tatCa = new ArrayList<>();
        tatCa.addAll(kh.moi());
        tatCa.addAll(kh.huy());
        tatCa.addAll(kh.capNhat());
        assertEquals(tatCa.size(), new HashSet<>(tatCa).size(), "mỗi khách chỉ nhận đúng một mail");
    }

    @Test
    void themKhachVaDoiThongTin_khachGiuLaiNhanCapNhatKhachThemNhanMoi() {
        khop(PhieuMailKeHoach.lap(KHACH_CU, List.of(2, 4, 5, 6), true), List.of(6), List.of(), List.of(2, 4, 5));
    }

    @Test
    void chiBotKhachThongTinKhongDoi_khachGiuLaiKhongNhanGi() {
        khop(PhieuMailKeHoach.lap(KHACH_CU, List.of(2, 5), false), List.of(), List.of(4), List.of());
    }

    @Test
    void khongLamThayDoiDanhSachDauVao() {
        List<Integer> cu = new ArrayList<>(KHACH_CU);
        List<Integer> moi = new ArrayList<>(List.of(1, 3, 5));
        PhieuMailKeHoach.lap(cu, moi, true);
        assertEquals(KHACH_CU, cu);
        assertEquals(List.of(1, 3, 5), moi);
    }
}
