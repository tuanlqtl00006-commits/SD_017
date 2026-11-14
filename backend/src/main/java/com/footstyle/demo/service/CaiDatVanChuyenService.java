package com.footstyle.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.exception.LoiForm;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Cài đặt miễn phí vận chuyển (free ship) theo giá trị đơn, quản lý sửa trên màn Cài đặt → Cài đặt free ship.
 * <ul>
 *   <li>Đang bật và tiền hàng của đơn (đã giảm theo đợt, CHƯA trừ phiếu giảm giá) từ ngưỡng trở lên thì phí giao hàng = 0.
 *       Cùng số tiền mà điều kiện "đơn tối thiểu" của phiếu giảm giá đang so.</li>
 *   <li>Chưa đạt ngưỡng (hoặc đang tắt) thì phí tính như cũ theo tỉnh nhận ({@link PhiVanChuyenTinh}).</li>
 * </ul>
 * Lưu ở tệp {@code <app.cai-dat-dir>/van-chuyen.json} trên máy chủ (CSDL không có bảng cài đặt, không đổi SQL): sửa xong có hiệu lực ngay,
 * khởi động lại vẫn còn. Chưa có tệp thì lấy mặc định ở application.properties (app.van-chuyen.mien-phi-bat / mien-phi-tu).
 * Phí của từng đơn đã lưu trong hóa đơn nên đổi cài đặt không làm đổi các đơn đã có.
 */
@Service
public class CaiDatVanChuyenService {

    private static final Logger log = LoggerFactory.getLogger(CaiDatVanChuyenService.class);

    /** Ngưỡng lớn nhất nhận được (bằng số tiền lớn nhất của một lần thanh toán tại quầy). */
    static final long NGUONG_TOI_DA = BanHangService.TIEN_TOI_DA;

    /** bat: có miễn phí theo giá trị đơn không; nguong: tiền hàng từ bao nhiêu thì miễn phí. */
    public record CaiDat(boolean bat, long nguong) {
    }

    private final ObjectMapper json = new ObjectMapper();
    private final Path tep;
    private volatile CaiDat hienTai;
    /** Lần quản lý lưu gần nhất (yyyy-MM-ddTHH:mm:ss); null = chưa ai sửa, đang dùng mức mặc định. */
    private volatile String capNhatLuc;

    @Autowired
    public CaiDatVanChuyenService(@Value("${app.cai-dat-dir:cai-dat}") String thuMuc,
                                  @Value("${app.van-chuyen.mien-phi-bat:true}") boolean batMacDinh,
                                  @Value("${app.van-chuyen.mien-phi-tu:2000000}") long nguongMacDinh) {
        this.tep = Path.of(thuMuc == null || thuMuc.isBlank() ? "cai-dat" : thuMuc.trim()).resolve("van-chuyen.json");
        this.hienTai = docTep(new CaiDat(batMacDinh, Math.max(1L, nguongMacDinh)));
    }

    private CaiDatVanChuyenService(CaiDat coDinh) {
        this.tep = null;
        this.hienTai = coDinh;
    }

    /** Cài đặt chỉ giữ trong bộ nhớ, không đọc / ghi tệp (dùng cho test). */
    public static CaiDatVanChuyenService trongBoNho(boolean bat, long nguong) {
        return new CaiDatVanChuyenService(new CaiDat(bat, nguong));
    }

    private CaiDat docTep(CaiDat macDinh) {
        try {
            if (!Files.isRegularFile(tep)) return macDinh;
            JsonNode n = json.readTree(tep.toFile());
            long nguong = n.path("mienPhiTu").asLong(macDinh.nguong());
            if (nguong < 1 || nguong > NGUONG_TOI_DA) nguong = macDinh.nguong();
            String luc = n.path("capNhatLuc").asText("");
            capNhatLuc = luc.isBlank() ? null : luc;
            return new CaiDat(n.path("mienPhiBat").asBoolean(macDinh.bat()), nguong);
        } catch (IOException | RuntimeException e) {
            log.warn("Không đọc được cài đặt vận chuyển ở {}, dùng mặc định: {}", tep, e.getMessage());
            return macDinh;
        }
    }

    public CaiDat lay() {
        return hienTai;
    }

    /** Đơn có tiền hàng này có được miễn phí vận chuyển theo giá trị đơn không. */
    public boolean mienPhi(long tienHang) {
        CaiDat c = hienTai;
        return c.bat() && tienHang >= c.nguong();
    }

    /** Phí giao hàng của đơn: đạt ngưỡng thì 0, chưa đạt thì giữ phí tính theo cách thường (theo tỉnh / nhân viên nhập). */
    public long phi(long tienHang, long phiThuong) {
        return mienPhi(tienHang) ? 0L : phiThuong;
    }

    /** Dạng trả về cho giao diện: { mienPhiBat, mienPhiTu }. */
    public Map<String, Object> moTa() {
        CaiDat c = hienTai;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mienPhiBat", c.bat());
        m.put("mienPhiTu", c.nguong());
        return m;
    }

    /** Dạng trả về cho màn Cài đặt của quản lý: thêm capNhatLuc (lần lưu gần nhất, null = chưa ai sửa). */
    public Map<String, Object> moTaQuanLy() {
        Map<String, Object> m = moTa();
        m.put("capNhatLuc", capNhatLuc);
        return m;
    }

    /**
     * Quản lý lưu cài đặt. Ngưỡng là số tiền nguyên từ 1 ₫ trở lên; đang tắt mà không gửi ngưỡng thì giữ ngưỡng cũ.
     * Ghi tệp xong mới đổi giá trị đang dùng (ghi lỗi thì cài đặt cũ vẫn nguyên).
     */
    public synchronized Map<String, Object> luu(Boolean bat, Long nguong) {
        LoiForm loi = new LoiForm();
        boolean batMoi = Boolean.TRUE.equals(bat);
        if (bat == null) loi.them("mienPhiBat", "Vui lòng chọn bật hoặc tắt miễn phí vận chuyển.");
        if (nguong == null) {
            if (batMoi) loi.them("mienPhiTu", "Vui lòng nhập giá trị đơn tối thiểu để được miễn phí vận chuyển.");
        } else if (nguong < 1) {
            loi.them("mienPhiTu", "Giá trị đơn tối thiểu phải lớn hơn 0.");
        } else if (nguong > NGUONG_TOI_DA) {
            loi.them("mienPhiTu", "Số tiền quá lớn.");
        }
        loi.nemNeuCoLoi();
        CaiDat moi = new CaiDat(batMoi, nguong != null ? nguong : hienTai.nguong());
        String luc = java.time.LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
        if (tep != null) ghiTep(moi, luc);
        hienTai = moi;
        capNhatLuc = luc;
        return moTa();
    }

    private void ghiTep(CaiDat c, String luc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mienPhiBat", c.bat());
        m.put("mienPhiTu", c.nguong());
        m.put("capNhatLuc", luc);
        try {
            Path thuMuc = tep.toAbsolutePath().getParent();
            Files.createDirectories(thuMuc);
            // Ghi ra tệp tạm rồi đổi tên để không bao giờ còn lại một tệp ghi dở
            Path tam = thuMuc.resolve("van-chuyen.json.tmp");
            json.writerWithDefaultPrettyPrinter().writeValue(tam.toFile(), m);
            Files.move(tam, tep.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Không ghi được tệp cài đặt vận chuyển {}", tep, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không lưu được cài đặt vận chuyển. Vui lòng thử lại.");
        }
    }
}
