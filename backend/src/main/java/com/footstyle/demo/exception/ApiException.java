package com.footstyle.demo.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ trả về cho frontend dưới dạng { "message": "..." }. */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;
    // Mã lỗi để frontend nhận biết trường hợp đặc biệt (vd "SAN_PHAM_DA_TON_TAI" -> hỏi người dùng có muốn cập nhật không)
    private final String code;
    private final Map<String, Object> chiTiet;

    public ApiException(HttpStatus status, String message) {
        this(status, message, null, null);
    }

    public ApiException(HttpStatus status, String message, String code, Map<String, Object> chiTiet) {
        super(message);
        this.status = status;
        this.code = code;
        this.chiTiet = chiTiet;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getChiTiet() {
        return chiTiet;
    }

    /** Dữ liệu đã tồn tại (409) kèm mã lỗi: frontend hỏi "bạn muốn cập nhật?" rồi gửi lại với xacNhanCapNhat = true. */
    public static ApiException daTonTai(String code, String message, Map<String, Object> chiTiet) {
        return new ApiException(HttpStatus.CONFLICT, message, code, chiTiet);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}
