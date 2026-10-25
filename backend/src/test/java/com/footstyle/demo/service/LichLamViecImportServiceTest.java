package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.service.LichLamViecImportService.KetQua;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LichLamViecImportServiceTest {

    private static final LocalDate HOM_NAY = CaLamMau.HOM_NAY;
    private static final List<Object> TIEU_DE = List.of("Mã nhân viên (*)", "Họ tên", "Ngày làm (*)", "Ca làm (*)", "Giờ", "Ghi chú");

    private final CaLamMau m = new CaLamMau();
    private final LichLamViecImportService dich;

    LichLamViecImportServiceTest() {
        LichLamViecService lich = new LichLamViecService(m.caRepo(), m.lichRepo(), m.nhanVienRepo(), m.giaoCaRepo());
        lich.datDongHo(CaLamMau.luc(HOM_NAY, "09:00"));
        dich = new LichLamViecImportService(lich, m.nhanVienRepo(), m.caRepo(), m.lichRepo());
    }

    private static String ngay(LocalDate d) {
        return String.format("%02d/%02d/%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    @SafeVarargs
    private static byte[] file(List<Object>... dong) {
        List<List<Object>> ds = new ArrayList<>();
        ds.add(TIEU_DE);
        ds.addAll(List.of(dong));
        return XlsxMau.tao(ds);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> loi(ApiException e) {
        return (List<Map<String, Object>>) e.getChiTiet().get("loi");
    }

    private ApiException tuChoi(byte[] f) {
        return assertThrows(ApiException.class, () -> dich.nhap(f, false));
    }

    @Test
    void fileHopLe_kiemTraKhongLuu_nhapThatThiLuuHet() {
        byte[] f = file(
                List.of("NV03", "Lê Quốc Bảo", ngay(HOM_NAY), "Ca sáng", "", "Mở cửa"),
                List.of("nv04", "", ngay(HOM_NAY.plusDays(1)), "CA02"),
                List.of("NV03", "", HOM_NAY.plusDays(2), "chiều"));
        KetQua xem = dich.nhap(f, true);
        assertFalse(xem.daLuu());
        assertEquals(3, xem.soLich());
        assertEquals(0, m.lichs.size(), "chỉ kiểm tra thì không ghi gì");

        KetQua kq = dich.nhap(f, false);
        assertTrue(kq.daLuu());
        assertEquals(3, m.lichs.size());
        assertEquals("Mở cửa", m.lichs.get(0).getGhiChu());
        assertEquals(m.anh, m.lichs.get(1).getNhanVien());
        assertEquals(m.chieu, m.lichs.get(2).getCaLamViec(), "\"chiều\" nhận ra Ca chiều; ngày dạng số serial của Excel cũng đọc được");
        assertNull(m.lichs.get(1).getGhiChu());
    }

    @Test
    void motDongSai_caFileBiTuChoi_khongLuuGi_baoDungDongVaCot() {
        ApiException e = tuChoi(file(
                List.of("NV03", "", ngay(HOM_NAY), "Ca sáng"),
                List.of("NV99", "", ngay(HOM_NAY), "Ca sáng"),
                List.of("NV10", "", ngay(HOM_NAY), "Ca sáng"),
                List.of("NV04", "Người Khác", ngay(HOM_NAY), "Ca sáng"),
                List.of("NV04", "", "32/13/2026", "Ca đêm"),
                List.of("NV04", "", ngay(HOM_NAY.minusDays(1)), "Ca tối"),
                List.of("NV99", "", ngay(HOM_NAY.minusDays(1)), "Ca tối")));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, e.getStatus());
        assertEquals(LichLamViecImportService.MA_LOI_FILE, e.getCode());
        assertEquals(0, m.lichs.size());
        List<Map<String, Object>> ds = loi(e);
        assertEquals(6, e.getChiTiet().get("soDongLoi"));
        assertEquals(2, ds.stream().filter(l -> l.get("dong").equals(8)).count(), "dòng sai cả mã NV lẫn ngày: báo cả hai lỗi cùng lúc");
        assertTrue(ds.stream().noneMatch(l -> l.get("dong").equals(2)), "dòng đúng không bị báo lỗi");
        assertTrue(ds.contains(Map.of("dong", 3, "truong", "maNhanVien", "cot", "Mã nhân viên (*)", "thongBao", "Không có nhân viên mã NV99.")));
        assertTrue(ds.stream().anyMatch(l -> l.get("dong").equals(4) && l.get("thongBao").toString().contains("ngừng hoạt động")));
        assertTrue(ds.stream().anyMatch(l -> l.get("dong").equals(5) && l.get("truong").equals("hoTen")));
        assertTrue(ds.stream().anyMatch(l -> l.get("dong").equals(6) && l.get("truong").equals("ngayLam")));
        assertTrue(ds.stream().anyMatch(l -> l.get("dong").equals(6) && l.get("thongBao").toString().startsWith("Không có ca \"Ca đêm\"")));
        assertTrue(ds.stream().anyMatch(l -> l.get("dong").equals(7) && l.get("truong").equals("ngayLam")
                && l.get("thongBao").toString().contains("Ngày đã qua")));
    }

    @Test
    void trungHoacChongGio_trongFile_vaVoiLichDaCo() {
        m.lich(m.anh, m.toi, HOM_NAY);
        ApiException e = tuChoi(file(
                List.of("NV03", "", ngay(HOM_NAY), "Ca sáng"),
                List.of("NV03", "", ngay(HOM_NAY), "Ca sáng"),
                List.of("NV03", "", ngay(HOM_NAY), "Ca gãy"),
                List.of("NV03", "", ngay(HOM_NAY), "Ca chiều"),
                List.of("NV04", "", ngay(HOM_NAY), "Ca tối")));
        List<Map<String, Object>> ds = loi(e);
        assertEquals(3, ds.size());
        assertEquals("Trùng với dòng 2 (cùng nhân viên, cùng ngày, cùng ca).", ds.get(0).get("thongBao"));
        assertEquals("Chồng giờ với Ca sáng ở dòng 2.", ds.get(1).get("thongBao"));
        assertEquals(6, ds.get(2).get("dong"));
        assertTrue(ds.get(2).get("thongBao").toString().startsWith("Đã có lịch Ca tối"));
        assertEquals(1, m.lichs.size());
    }

    @Test
    void thieuCot_fileTrong_caNgungDung() {
        ApiException thieu = assertThrows(ApiException.class, () -> dich.nhap(XlsxMau.tao(List.of("Mã nhân viên", "Ca làm"), List.of("NV03", "Ca sáng")), true));
        assertEquals(HttpStatus.BAD_REQUEST, thieu.getStatus());
        assertTrue(thieu.getMessage().contains("Ngày làm (*)"));

        ApiException chiTieuDe = assertThrows(ApiException.class, () -> dich.nhap(XlsxMau.tao(TIEU_DE), true));
        assertTrue(chiTieuDe.getMessage().contains("chưa có dòng lịch nào"));

        m.sang.setTrangThai(0);
        ApiException tat = tuChoi(file(List.of("NV03", "", ngay(HOM_NAY), "Ca sáng")));
        assertEquals("Ca \"Ca sáng\" đang ngừng sử dụng.", loi(tat).get(0).get("thongBao"));
    }

    @Test
    void timCa_theoMaHoacTen_khongPhanBietDau() {
        assertEquals(m.sang, LichLamViecImportService.timCa(m.cas, "ca sang"));
        assertEquals(m.chieu, LichLamViecImportService.timCa(m.cas, "CA02"));
        assertEquals(m.toi, LichLamViecImportService.timCa(m.cas, "Tối"));
        assertNull(LichLamViecImportService.timCa(m.cas, "Ca khuya"));
    }
}
