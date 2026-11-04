package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PhiVanChuyenTinhTest {

    @Test
    void haNoiMienPhi_moiCachGhiTen() {
        assertEquals(0L, PhiVanChuyenTinh.phi("Hà Nội"));
        assertEquals(0L, PhiVanChuyenTinh.phi("Thành phố Hà Nội"));
        assertEquals(0L, PhiVanChuyenTinh.phi("  TP. Hà Nội "));
        assertEquals(0L, PhiVanChuyenTinh.phi("ha noi"));
    }

    @Test
    void tinhKhacMatPhi_moiTinhMotGia() {
        assertEquals(25_000L, PhiVanChuyenTinh.phi("Tỉnh Bắc Ninh"));
        assertEquals(30_000L, PhiVanChuyenTinh.phi("Tỉnh Quảng Ninh"));
        assertEquals(35_000L, PhiVanChuyenTinh.phi("Tỉnh Nghệ An"));
        assertEquals(40_000L, PhiVanChuyenTinh.phi("Thành phố Đà Nẵng"));
        assertEquals(45_000L, PhiVanChuyenTinh.phi("Tỉnh Lâm Đồng"));
        assertEquals(50_000L, PhiVanChuyenTinh.phi("Thành phố Hồ Chí Minh"));
        assertEquals(50_000L, PhiVanChuyenTinh.phi("Thành phố Đồng Nai"));
        assertEquals(55_000L, PhiVanChuyenTinh.phi("Thành phố Cần Thơ"));
        assertEquals(55_000L, PhiVanChuyenTinh.phi("Tỉnh Cà Mau"));
    }

    @Test
    void tinhKhongNhanRa_hoacTrong_tinhPhiMacDinh() {
        assertEquals(PhiVanChuyenTinh.PHI_MAC_DINH, PhiVanChuyenTinh.phi("Tỉnh Không Có"));
        assertEquals(PhiVanChuyenTinh.PHI_MAC_DINH, PhiVanChuyenTinh.phi(""));
        assertEquals(PhiVanChuyenTinh.PHI_MAC_DINH, PhiVanChuyenTinh.phi(null));
    }

    @Test
    void phiTheoDiaChi_layTinhOCuoiDiaChi() {
        assertEquals(0L, PhiVanChuyenTinh.phiTheoDiaChi("Số 15 Trần Phú, Phường Hà Đông, Thành phố Hà Nội"));
        assertEquals(50_000L, PhiVanChuyenTinh.phiTheoDiaChi("xcv, Phường Bảo Vinh, Thành phố Đồng Nai"));
        assertNull(PhiVanChuyenTinh.phiTheoDiaChi("Số 15 Trần Phú, khu phố 3"));
        assertNull(PhiVanChuyenTinh.phiTheoDiaChi(" "));
        assertNull(PhiVanChuyenTinh.phiTheoDiaChi(null));
    }

    @Test
    void baoPhuDu34TinhThanh() {
        String[] ds = {"Hà Nội", "Bắc Ninh", "Hưng Yên", "Phú Thọ", "Thái Nguyên", "Hải Phòng", "Ninh Bình", "Quảng Ninh", "Lào Cai",
                "Tuyên Quang", "Lạng Sơn", "Cao Bằng", "Điện Biên", "Lai Châu", "Sơn La", "Thanh Hóa", "Nghệ An", "Hà Tĩnh", "Quảng Trị",
                "Huế", "Đà Nẵng", "Quảng Ngãi", "Gia Lai", "Khánh Hòa", "Đắk Lắk", "Lâm Đồng", "Hồ Chí Minh", "Đồng Nai", "Tây Ninh",
                "Cần Thơ", "Vĩnh Long", "Đồng Tháp", "Cà Mau", "An Giang"};
        assertEquals(34, ds.length);
        for (String t : ds) assertTrue(PhiVanChuyenTinh.nhanRa(t), t);
    }
}
