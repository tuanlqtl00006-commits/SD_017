package com.footstyle.demo.exception;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Gom lỗi theo từng ô của biểu mẫu: mỗi ô giữ lỗi đầu tiên, lỗi trùng -> 409, còn lại -> 400. */
class LoiFormTest {

    @Test
    void bamDauChuaCoLoi() {
        LoiForm lf = new LoiForm();
        assertFalse(lf.coLoi());
        assertNull(lf.loiDauTien());
        assertTrue(lf.theoTruong().isEmpty());
        assertDoesNotThrow(lf::nemNeuCoLoi);
    }

    @Test
    void moiOChiGiuLoiDauTien() {
        LoiForm lf = new LoiForm().them("email", "Nhập email.").them("email", "Email sai định dạng.").trung("email", "Email đã được sử dụng.");
        assertEquals(Map.of("email", "Nhập email."), lf.theoTruong());
        assertTrue(lf.co("email"));
        assertFalse(lf.co("hoTen"));
    }

    @Test
    void nhieuOCungLucTheoThuTuGhi() {
        LoiForm lf = new LoiForm().them("hoTen", "Nhập họ tên.").them("sdt", "Số điện thoại sai.").them("email", "Nhập email.");
        assertEquals(List.of("hoTen", "sdt", "email"), List.copyOf(lf.theoTruong().keySet()));
        assertEquals("Nhập họ tên.", lf.loiDauTien());
    }

    @Test
    void neuChiGhiKhiDieuKienDung() {
        LoiForm lf = new LoiForm().neu(false, "a", "A sai.").neu(true, "b", "B sai.");
        assertEquals(Map.of("b", "B sai."), lf.theoTruong());
    }

    @Test
    void theoTruongTraBanSaoKhongSuaDuocBenTrong() {
        LoiForm lf = new LoiForm().them("a", "x");
        lf.theoTruong().put("b", "y");
        assertEquals(1, lf.theoTruong().size());
    }

    @Test
    void loiDauLaFormatThi400DuCoLoiTrungPhiaSau() {
        LoiForm lf = new LoiForm().them("hoTen", "Nhập họ tên.").trung("email", "Email đã được sử dụng.");
        ApiException e = assertThrows(ApiException.class, lf::nemNeuCoLoi);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        assertEquals("Nhập họ tên.", e.getMessage());
        assertEquals(Map.of("hoTen", "Nhập họ tên.", "email", "Email đã được sử dụng."), e.getLoiTheoTruong());
    }

    @Test
    void loiDauLaTrungThi409DuCoLoiFormatPhiaSau() {
        LoiForm lf = new LoiForm().trung("email", "Email đã được sử dụng.").them("hoTen", "Nhập họ tên.");
        ApiException e = assertThrows(ApiException.class, lf::nemNeuCoLoi);
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals("Email đã được sử dụng.", e.getMessage());
        assertEquals(2, e.getLoiTheoTruong().size());
    }

    @Test
    void oDaCoLoiThiLoiTrungSauKhongDoiMa() {
        // email đã có lỗi format (400) -> lỗi "trùng" ghi sau bị bỏ qua, mã vẫn là 400
        LoiForm lf = new LoiForm().them("email", "Email sai định dạng.").trung("email", "Email đã được sử dụng.");
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class, lf::nemNeuCoLoi).getStatus());
    }

    @Test
    void apiExceptionThongThuongKhongCoLoiTheoO() {
        assertNull(ApiException.badRequest("x").getLoiTheoTruong());
        assertNull(ApiException.conflict("x").getLoiTheoTruong());
        assertNull(ApiException.notFound("x").getLoiTheoTruong());
    }
}
