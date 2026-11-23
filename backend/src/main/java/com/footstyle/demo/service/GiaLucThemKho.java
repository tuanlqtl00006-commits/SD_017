package com.footstyle.demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Giá niêm yết + % đợt giảm giá của từng dòng hóa đơn chờ LÚC THÊM (theo id dòng hoa_don_chi_tiet).
 * CSDL chỉ lưu đơn giá sau giảm và không được thêm cột (cấm đổi SQL), nên lưu ở tệp {@code <app.cai-dat-dir>/gia-luc-them.json}
 * giống cài đặt vận chuyển: khởi động lại backend vẫn nhớ, dòng cũ luôn hiện đúng giá gốc gạch + % của đợt lúc thêm (21/11).
 * Mỗi máy một tệp (thư mục cai-dat/ không đẩy lên git). duongDan = null: chỉ nhớ trong bộ nhớ (dùng cho test).
 */
@Component
public class GiaLucThemKho {

    private static final Logger log = LoggerFactory.getLogger(GiaLucThemKho.class);

    /**
     * donGia: đơn giá dòng lúc nhớ; giaGoc: giá niêm yết lúc thêm; phanTram: % đợt giảm giá lúc thêm.
     * giaTruoc (có thể null): giá của dòng cùng biến thể ngay trước khi giá đổi - dòng này được tạo / chuyển sang giá mới
     * từ mức đó. Chữ đỏ "giá cũ → giá mới" dựa vào số này nên xóa dòng giá cũ vẫn còn (góp ý 23/11).
     */
    public record GiaLucThem(long donGia, long giaGoc, double phanTram, Long giaTruoc) {
        public GiaLucThem(long donGia, long giaGoc, double phanTram) {
            this(donGia, giaGoc, phanTram, null);
        }
    }

    private final Path tep;
    private final ObjectMapper json = new ObjectMapper();
    private final Map<Integer, GiaLucThem> nho = new ConcurrentHashMap<>();

    // app.gia-luc-them-dir: mặc định cùng thư mục cài đặt; test chạy trên CSDL thật đặt sang target/ để không ghi vào tệp của máy
    public GiaLucThemKho(@Value("${app.gia-luc-them-dir:${app.cai-dat-dir:cai-dat}}") String thuMuc) {
        this.tep = thuMuc == null ? null : Path.of(thuMuc.isBlank() ? "cai-dat" : thuMuc.trim()).resolve("gia-luc-them.json");
        doc();
    }

    /** Kho chỉ nhớ trong bộ nhớ (không ghi tệp). */
    public static GiaLucThemKho trongBoNho() {
        return new GiaLucThemKho(null);
    }

    public GiaLucThem lay(Integer idDong) {
        return idDong == null ? null : nho.get(idDong);
    }

    public boolean coDong(Integer idDong) {
        return idDong != null && nho.containsKey(idDong);
    }

    public void luu(Integer idDong, GiaLucThem g) {
        if (idDong == null || g == null) return;
        sauCommit(() -> {
            nho.put(idDong, g);
            ghi();
        });
    }

    public void xoa(Integer idDong) {
        if (idDong != null) xoaNhieu(List.of(idDong));
    }

    /** Bỏ các dòng không còn ở hóa đơn chờ (bỏ / gộp dòng, hủy hóa đơn chờ, đã thanh toán) - ghi tệp một lần. */
    public void xoaNhieu(Collection<Integer> idDong) {
        if (idDong == null || idDong.isEmpty()) return;
        sauCommit(() -> {
            boolean doi = false;
            for (Integer id : idDong) doi |= id != null && nho.remove(id) != null;
            if (doi) ghi();
        });
    }

    /**
     * Đang trong giao dịch CSDL thì chỉ áp dụng sau khi commit: giao dịch lỗi / rollback (kể cả test chạy trên CSDL thật)
     * thì tệp không bị ghi lệch với CSDL. Không có giao dịch thì làm ngay.
     */
    private static void sauCommit(Runnable viec) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    viec.run();
                }
            });
        } else {
            viec.run();
        }
    }

    private void doc() {
        if (tep == null || !Files.exists(tep)) return;
        try {
            Map<Integer, GiaLucThem> daLuu = json.readValue(tep.toFile(), new TypeReference<Map<Integer, GiaLucThem>>() { });
            if (daLuu != null) nho.putAll(daLuu);
            log.info("Giá lúc thêm của hóa đơn chờ: đã nạp {} dòng từ {}.", nho.size(), tep.toAbsolutePath());
        } catch (IOException e) {
            // tệp hỏng: bỏ qua, dòng cũ sẽ dò lại giá gốc theo đợt (HoaDonChoService.giaGocChoDong)
            log.warn("Không đọc được {} ({}), bỏ qua giá lúc thêm đã nhớ.", tep, e.getMessage());
        }
    }

    /** Ghi ra tệp tạm rồi đổi tên, tránh tệp bị cụt khi tắt máy giữa chừng. */
    private synchronized void ghi() {
        if (tep == null) return;
        try {
            if (tep.getParent() != null) Files.createDirectories(tep.getParent());
            Path tam = tep.resolveSibling(tep.getFileName() + ".tmp");
            json.writerWithDefaultPrettyPrinter().writeValue(tam.toFile(), new TreeMap<>(nho));
            Files.move(tam, tep, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.warn("Không ghi được {}: {}", tep, e.getMessage());
        }
    }
}
