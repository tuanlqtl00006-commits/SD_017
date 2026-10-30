package com.footstyle.demo.controller;

import com.footstyle.demo.service.HoaDonSuaService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Nhận thông báo "tiền vào tài khoản" từ cổng thanh toán (SePay: https://sepay.vn - cấu hình Webhooks, kiểu xác thực "API Key").
 * Đường dẫn công khai nên BẮT BUỘC kiểm tra khóa bí mật: chưa cấu hình khóa thì từ chối mọi yêu cầu.
 * Nội dung chuyển khoản chứa mã đơn (VD "HD26") -> tự ghi khoản đã thanh toán, nhân viên không phải đối chiếu sao kê.
 */
@RestController
@RequestMapping("/api/cua-hang/webhook")
@RequiredArgsConstructor
public class ThanhToanWebhookController {

    private static final Logger log = LoggerFactory.getLogger(ThanhToanWebhookController.class);
    private static final Pattern MA_DON = Pattern.compile("HD\\d+", Pattern.CASE_INSENSITIVE);

    private final HoaDonSuaService hoaDonSuaService;

    @Value("${app.thanh-toan.webhook-key:}")
    private String webhookKey;

    @PostMapping("/sepay")
    public ResponseEntity<Map<String, Object>> sepay(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        if (webhookKey == null || webhookKey.isBlank()) {
            return phanHoi(HttpStatus.SERVICE_UNAVAILABLE, false, "Chưa cấu hình app.thanh-toan.webhook-key.");
        }
        String nhan = authorization == null ? "" : authorization.trim();
        String khoa = nhan.regionMatches(true, 0, "Apikey ", 0, 7) ? nhan.substring(7).trim() : nhan;
        if (!MessageDigest.isEqual(khoa.getBytes(StandardCharsets.UTF_8), webhookKey.trim().getBytes(StandardCharsets.UTF_8))) {
            return phanHoi(HttpStatus.UNAUTHORIZED, false, "Sai khóa xác thực.");
        }
        // Chỉ xử lý tiền VÀO tài khoản
        if (!"in".equalsIgnoreCase(String.valueOf(body.getOrDefault("transferType", "in")))) {
            return phanHoi(HttpStatus.OK, true, "Bỏ qua giao dịch tiền ra.");
        }
        long soTien;
        try {
            soTien = Math.round(Double.parseDouble(String.valueOf(body.get("transferAmount"))));
        } catch (NumberFormatException | NullPointerException e) {
            return phanHoi(HttpStatus.BAD_REQUEST, false, "Số tiền không hợp lệ.");
        }
        String noiDung = String.valueOf(body.getOrDefault("content", ""));
        String maGd = body.get("referenceCode") != null ? String.valueOf(body.get("referenceCode")) : String.valueOf(body.getOrDefault("id", ""));

        // Nội dung ngân hàng có thể kèm chữ khác: thử từng mã dạng HDxxx cho tới khi khớp một đơn thật
        String kq = "Không thấy mã đơn trong nội dung chuyển khoản.";
        Matcher m = MA_DON.matcher(noiDung);
        while (m.find()) {
            kq = hoaDonSuaService.ghiNhanChuyenKhoanTuDong(m.group().toUpperCase(), soTien, maGd, noiDung);
            if (kq.startsWith("Đã ghi nhận") || kq.contains("đã ghi nhận trước đó")) break;
        }
        log.info("Webhook thanh toán: {} đ, nội dung='{}' -> {}", soTien, noiDung, kq);
        return phanHoi(HttpStatus.OK, true, kq);
    }

    private static ResponseEntity<Map<String, Object>> phanHoi(HttpStatus st, boolean ok, String msg) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", ok);
        r.put("message", msg);
        return ResponseEntity.status(st).body(r);
    }
}
