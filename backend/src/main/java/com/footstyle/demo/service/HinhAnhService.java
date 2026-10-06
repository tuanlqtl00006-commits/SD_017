package com.footstyle.demo.service;

import com.footstyle.demo.exception.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lưu ảnh sản phẩm do admin chọn từ máy.
 * Luồng: nhận file -> kiểm tra dung lượng + định dạng THẬT (đọc vài byte đầu, không tin đuôi file / content-type)
 * -> lưu vào thư mục upload với tên ngẫu nhiên -> trả đường dẫn "/api/uploads/ten-file".
 * Đường dẫn này được lưu vào cột hinh_anh_san_pham.duong_dan_hinh_anh khi lưu sản phẩm.
 */
@Service
public class HinhAnhService {

    /** Tiền tố đường dẫn công khai của ảnh đã tải lên (khớp với WebConfig). */
    public static final String TIEN_TO_URL = "/api/uploads/";

    private static final long TOI_DA_BYTE = 5L * 1024 * 1024; // 5 MB

    private final Path thuMuc;

    public HinhAnhService(@Value("${app.upload-dir:uploads}") String thuMucUpload) {
        this.thuMuc = Paths.get(thuMucUpload).toAbsolutePath().normalize();
    }

    public String luu(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Chưa chọn ảnh.");
        }
        if (file.getSize() > TOI_DA_BYTE) {
            throw ApiException.badRequest("Ảnh quá lớn, tối đa 5 MB.");
        }
        try {
            byte[] noiDung = file.getBytes();
            String duoi = docDinhDang(noiDung);
            if (duoi == null) {
                throw ApiException.badRequest("Chỉ nhận ảnh JPG, PNG, WEBP hoặc GIF.");
            }
            Files.createDirectories(thuMuc);
            String ten = UUID.randomUUID().toString().replace("-", "") + "." + duoi;
            Files.write(thuMuc.resolve(ten), noiDung);
            return TIEN_TO_URL + ten;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không lưu được ảnh. Vui lòng thử lại.");
        }
    }

    /** Nhận dạng định dạng ảnh qua các byte đầu file. Trả về đuôi file, hoặc null nếu không phải ảnh được hỗ trợ. */
    static String docDinhDang(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "png";
        }
        if (b.length >= 4 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return "gif";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
