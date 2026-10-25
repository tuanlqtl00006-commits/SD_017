package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.GiaoCa;
import com.footstyle.demo.entity.LichLamViec;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.security.XacThuc;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LichLamViecServiceTest {

    private final CaLamMau m = new CaLamMau();
    private final LichLamViecService service;
    private static final LocalDate HOM_NAY = CaLamMau.HOM_NAY;

    LichLamViecServiceTest() {
        service = new LichLamViecService(m.caRepo(), m.lichRepo(), m.nhanVienRepo(), m.giaoCaRepo());
        service.datDongHo(CaLamMau.luc(HOM_NAY, "09:00"));
    }

    @AfterEach
    void don() {
        XacThuc.xoa();
    }

    private static ApiException loi(Runnable r) {
        return assertThrows(ApiException.class, r::run);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> boQua(Map<String, Object> kq) {
        return (List<Map<String, Object>>) kq.get("boQua");
    }

    @Test
    void haiCaChongGio() {
        assertFalse(LichLamViecService.chongGio(m.sang, m.chieu), "07-12 và 12-17 nối tiếp, không chồng");
        assertTrue(LichLamViecService.chongGio(m.sang, m.caGay));
        assertTrue(LichLamViecService.chongGio(m.caGay, m.chieu));
        assertFalse(LichLamViecService.chongGio(m.sang, m.toi));
        assertTrue(LichLamViecService.chongGio(m.sang, m.sang));
    }

    @Test
    void xepLich_boQuaNgayDaQua_conLaiThemDuoc() {
        Map<String, Object> kq = service.xepLich(new LichLamViecService.XepLichRequest(List.of(3, 4), 1,
                List.of(HOM_NAY.minusDays(1).toString(), HOM_NAY.toString(), HOM_NAY.plusDays(1).toString()), " Mở cửa "));
        assertEquals(4, kq.get("daThem"));
        assertEquals(2, boQua(kq).size());
        assertTrue(boQua(kq).stream().allMatch(b -> b.get("lyDo").toString().contains("Ngày đã qua")));
        assertEquals("Mở cửa", m.lichs.get(0).getGhiChu());
    }

    @Test
    void xepLich_trungHoacChongGio_bichanTungO() {
        service.xepLich(new LichLamViecService.XepLichRequest(List.of(3), 1, List.of(HOM_NAY.toString()), null));
        Map<String, Object> lai = service.xepLich(new LichLamViecService.XepLichRequest(List.of(3), 1, List.of(HOM_NAY.toString()), null));
        assertEquals(0, lai.get("daThem"));
        assertTrue(boQua(lai).get(0).get("lyDo").toString().startsWith("Đã có lịch Ca sáng"));
        Map<String, Object> gay = service.xepLich(new LichLamViecService.XepLichRequest(List.of(3), 4, List.of(HOM_NAY.toString()), null));
        assertEquals("Trùng giờ với Ca sáng (07:00 - 12:00).", boQua(gay).get(0).get("lyDo"));
        Map<String, Object> chieu = service.xepLich(new LichLamViecService.XepLichRequest(List.of(3), 2, List.of(HOM_NAY.toString()), null));
        assertEquals(1, chieu.get("daThem"), "ca chiều nối tiếp ca sáng vẫn xếp được");
    }

    @Test
    void xepLich_nhanVienNghiViec_vaCaNgungDung() {
        Map<String, Object> kq = service.xepLich(new LichLamViecService.XepLichRequest(List.of(10, 99), 1, List.of(HOM_NAY.toString()), null));
        assertEquals(0, kq.get("daThem"));
        assertEquals("Nhân viên đang ngừng hoạt động.", boQua(kq).get(0).get("lyDo"));
        assertEquals("Không tìm thấy nhân viên.", boQua(kq).get(1).get("lyDo"));
        m.toi.setTrangThai(0);
        ApiException e = loi(() -> service.xepLich(new LichLamViecService.XepLichRequest(List.of(3), 3, List.of(HOM_NAY.toString()), null)));
        assertTrue(e.getMessage().contains("ngừng sử dụng"));
    }

    @Test
    void xepLich_thieuThongTin_baoTheoO() {
        ApiException e = loi(() -> service.xepLich(new LichLamViecService.XepLichRequest(List.of(), null, List.of(), null)));
        assertEquals(List.of("idNhanViens", "idCa", "ngays"), List.copyOf(e.getLoiTheoTruong().keySet()));
        e = loi(() -> service.xepLich(new LichLamViecService.XepLichRequest(List.of(3), 1, List.of("16/10/2026"), null)));
        assertTrue(e.getLoiTheoTruong().get("ngays").contains("yyyy-MM-dd"));
        e = loi(() -> service.xepLich(new LichLamViecService.XepLichRequest(List.of(3), 1, List.of(HOM_NAY.toString()), "x".repeat(256))));
        assertTrue(e.getLoiTheoTruong().containsKey("ghiChu"));
    }

    @Test
    void xepLich_quaNhieuO_bichan() {
        List<Integer> nvs = java.util.stream.IntStream.rangeClosed(1, 30).boxed().toList();
        List<String> ngays = java.util.stream.IntStream.range(0, 20).mapToObj(i -> HOM_NAY.plusDays(i).toString()).toList();
        ApiException e = loi(() -> service.xepLich(new LichLamViecService.XepLichRequest(nvs, 1, ngays, null)));
        assertTrue(e.getLoiTheoTruong().get("ngays").contains("500"));
    }

    @Test
    void xoaLich_ngayDaQuaHoacDaNhanCa_khongXoaDuoc() {
        LichLamViec qua = m.lich(m.bao, m.sang, HOM_NAY.minusDays(1));
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.xoaLich(qua.getId())).getStatus());
        LichLamViec homNay = m.lich(m.bao, m.sang, HOM_NAY);
        GiaoCa gc = new GiaoCa();
        gc.setId(1);
        gc.setNhanVien(m.bao);
        gc.setLichLamViec(homNay);
        gc.setTrangThai(1);
        m.giaoCas.add(gc);
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.xoaLich(homNay.getId())).getStatus());
        LichLamViec mai = m.lich(m.anh, m.chieu, HOM_NAY.plusDays(1));
        service.xoaLich(mai.getId());
        assertFalse(m.lichs.contains(mai));
    }

    @Test
    void saoChepTuan_sangTuanSau_chayLaiKhongNhanDoi() {
        LocalDate thu2 = LocalDate.of(2026, 10, 12);
        m.lich(m.bao, m.sang, thu2);
        m.lich(m.bao, m.chieu, thu2.plusDays(4));
        m.lich(m.nghi, m.toi, thu2.plusDays(1));
        Map<String, Object> kq = service.saoChepTuan(thu2, thu2.plusDays(7));
        assertEquals(2, kq.get("daThem"));
        assertEquals("Nhân viên đang ngừng hoạt động.", boQua(kq).get(0).get("lyDo"));
        assertTrue(m.lichs.stream().anyMatch(l -> l.getNgayLam().equals(LocalDate.of(2026, 10, 19)) && l.getCaLamViec() == m.sang));
        Map<String, Object> lai = service.saoChepTuan(thu2, thu2.plusDays(7));
        assertEquals(0, lai.get("daThem"));
    }

    @Test
    void saoChepTuan_saiThu2_tuanDaQua_tuanNguonRong() {
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.saoChepTuan(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 19))).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.saoChepTuan(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 5))).getStatus());
        assertTrue(loi(() -> service.saoChepTuan(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9))).getMessage().contains("chưa có lịch"));
    }

    @Test
    void xemLich_khoangNgayHopLe() {
        m.lich(m.bao, m.chieu, HOM_NAY);
        m.lich(m.anh, m.sang, HOM_NAY);
        m.lich(m.bao, m.sang, HOM_NAY.minusDays(2));
        List<Map<String, Object>> ds = service.lich(HOM_NAY.minusDays(3), HOM_NAY.plusDays(3), null);
        assertEquals(3, ds.size());
        assertEquals(true, ds.get(0).get("daQua"));
        assertEquals("Ca sáng", ds.get(1).get("tenCa"), "trong ngày sắp theo giờ bắt đầu");
        assertEquals("07:00", ds.get(1).get("gioBatDau"));
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.lich(HOM_NAY, HOM_NAY.minusDays(1), null)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, loi(() -> service.lich(HOM_NAY, HOM_NAY.plusDays(70), null)).getStatus());
        CaLamMau.dangNhap(m.bao);
        assertEquals(2, service.lichCuaToi(HOM_NAY.minusDays(3), HOM_NAY.plusDays(3)).size(), "chỉ lịch của mình");
    }

    @Test
    void themSuaCa_kiemTraTungO_maTuTang() {
        ApiException e = loi(() -> service.themCa(new LichLamViecService.CaRequest(" ", "25:00", "")));
        assertEquals(List.of("tenCa", "gioBatDau", "gioKetThuc"), List.copyOf(e.getLoiTheoTruong().keySet()));
        e = loi(() -> service.themCa(new LichLamViecService.CaRequest("Ca chiều muộn", "20:00", "15:00")));
        assertTrue(e.getLoiTheoTruong().get("gioKetThuc").contains("qua đêm"));
        e = loi(() -> service.themCa(new LichLamViecService.CaRequest("Ca đêm", "03:00", "23:00")));
        assertTrue(e.getLoiTheoTruong().get("gioBatDau").contains("giờ mở cửa"), "3 giờ sáng cửa hàng chưa mở");
        assertTrue(e.getLoiTheoTruong().get("gioKetThuc").contains("giờ mở cửa"), "11 giờ đêm cửa hàng đã đóng");
        e = loi(() -> service.themCa(new LichLamViecService.CaRequest("Ca ngắn", "08:00", "08:20")));
        assertTrue(e.getLoiTheoTruong().get("gioKetThuc").contains("30 phút"));
        assertEquals(HttpStatus.CONFLICT, loi(() -> service.themCa(new LichLamViecService.CaRequest("ca SÁNG", "07:00", "08:00"))).getStatus());
        Map<String, Object> moi = service.themCa(new LichLamViecService.CaRequest("  Ca   hành chính ", "08:00", "17:00:00"));
        assertEquals("CA05", moi.get("maCa"));
        assertEquals("Ca hành chính", moi.get("tenCa"));
        assertEquals("17:00", moi.get("gioKetThuc"));
        Map<String, Object> sua = service.suaCa(1, new LichLamViecService.CaRequest("Ca sáng", "07:30", "11:30"));
        assertEquals("07:30", sua.get("gioBatDau"), "sửa chính nó không bị báo trùng tên");
        assertEquals(false, service.doiTrangThaiCa(1).get("hoatDong"));
    }
}
