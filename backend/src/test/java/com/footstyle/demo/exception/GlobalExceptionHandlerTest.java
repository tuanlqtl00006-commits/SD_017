package com.footstyle.demo.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Nội dung JSON mà giao diện nhận được khi backend báo lỗi: message, loiTheoTruong (lỗi từng ô), code + chiTiet (lỗi file Excel). */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void loiThuongChiCoMessage() {
        ResponseEntity<Map<String, Object>> r = handler.handleApi(ApiException.badRequest("Thiếu dữ liệu."));
        assertEquals(400, r.getStatusCode().value());
        assertEquals(Map.of("message", "Thiếu dữ liệu."), r.getBody());
    }

    @Test
    void loiTheoTungOTraVeMapLoiTheoTruong_va400() {
        LoiForm lf = new LoiForm().them("hoTen", "Nhập họ tên.").them("ngaySinh", "Khách hàng phải từ đủ 15 tuổi.");
        ApiException e = assertThrows(ApiException.class, lf::nemNeuCoLoi);
        ResponseEntity<Map<String, Object>> r = handler.handleApi(e);
        assertEquals(400, r.getStatusCode().value());
        assertEquals("Nhập họ tên.", r.getBody().get("message"));
        assertEquals(Map.of("hoTen", "Nhập họ tên.", "ngaySinh", "Khách hàng phải từ đủ 15 tuổi."), r.getBody().get("loiTheoTruong"));
        assertFalse(r.getBody().containsKey("code"));
    }

    @Test
    void loiTrungTra409KemLoiTheoO() {
        LoiForm lf = new LoiForm().trung("email", "Email đã được sử dụng.").trung("sdt", "Số điện thoại đã được sử dụng.");
        ResponseEntity<Map<String, Object>> r = handler.handleApi(assertThrows(ApiException.class, lf::nemNeuCoLoi));
        assertEquals(409, r.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, String> theoO = (Map<String, String>) r.getBody().get("loiTheoTruong");
        assertEquals(2, theoO.size());
        assertEquals("Email đã được sử dụng.", theoO.get("email"));
    }

    @Test
    void loiFileExcelTra422KemMaVaChiTiet() {
        Map<String, Object> chiTiet = Map.of("loi", List.of(Map.of("dong", 3, "truong", "email", "cot", "Email", "thongBao", "Email đã được sử dụng.")),
                "tongDong", 5, "soDongLoi", 1);
        ApiException e = new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "File có 1 lỗi.", "FILE_KHONG_HOP_LE", chiTiet);
        ResponseEntity<Map<String, Object>> r = handler.handleApi(e);
        assertEquals(422, r.getStatusCode().value());
        assertEquals("FILE_KHONG_HOP_LE", r.getBody().get("code"));
        assertEquals(chiTiet, r.getBody().get("chiTiet"));
        assertFalse(r.getBody().containsKey("loiTheoTruong"));
    }

    @Test
    void coMaNhungKhongChiTietThiChiTietLaMapRong() {
        ResponseEntity<Map<String, Object>> r = handler.handleApi(new ApiException(HttpStatus.CONFLICT, "Đã có.", "DA_TON_TAI", null));
        assertEquals(Map.of(), r.getBody().get("chiTiet"));
    }

    @Test
    void loiTheoOKhacRongMoiDuocGui() {
        ApiException e = new ApiException(HttpStatus.BAD_REQUEST, "x", null, null, Map.of());
        assertFalse(handler.handleApi(e).getBody().containsKey("loiTheoTruong"));
        assertNotNull(handler.handleApi(new ApiException(HttpStatus.BAD_REQUEST, "x", null, null, Map.of("a", "b"))).getBody().get("loiTheoTruong"));
    }
}
