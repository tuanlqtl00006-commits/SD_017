package com.footstyle.demo.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", ex.getMessage());
        if (ex.getCode() != null) {
            body.put("code", ex.getCode());
            body.put("chiTiet", ex.getChiTiet() == null ? Map.of() : ex.getChiTiet());
        }
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Dữ liệu gửi lên không hợp lệ. Vui lòng kiểm tra lại các trường đã nhập."));
    }

    /** Ảnh vượt giới hạn spring.servlet.multipart.max-file-size. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Ảnh quá lớn, tối đa 5 MB."));
    }

    /** Trùng khóa duy nhất (vd 2 người cùng tạo một mã) hoặc vi phạm khóa ngoại. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Dữ liệu bị trùng hoặc đang được sử dụng ở nơi khác. Vui lòng tải lại trang và thử lại."));
    }

    /**
     * Lỗi CSDL khác (thiếu cột / thiếu bảng do CSDL cũ, mất kết nối...). Chi tiết SQL chỉ ghi vào log backend,
     * không trả nguyên văn câu SQL ra giao diện.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDataAccess(DataAccessException ex) {
        log.error("Lỗi truy cập CSDL", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Lỗi cơ sở dữ liệu. Hãy chạy phần 6 (Nâng cấp CSDL cũ) của database/SD_17_tong.sql (hoặc khởi động lại backend) rồi thử lại."));
    }
}
