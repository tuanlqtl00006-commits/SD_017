package com.footstyle.demo.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Gom lỗi của MỘT biểu mẫu theo từng ô nhập (tên ô -> câu báo lỗi), thay vì dừng ở lỗi đầu tiên.
 * Nhờ vậy giao diện hiện được tất cả lỗi cùng lúc, mỗi lỗi nằm dưới đúng ô của nó (họ tên, số điện thoại, email, ngày sinh...).
 *
 * Mỗi ô chỉ giữ lỗi ĐẦU TIÊN (các luật của một ô được kiểm tra từ cơ bản đến khó). Lỗi đầu tiên của cả biểu mẫu
 * cũng là câu "message" của ApiException (để chỗ nào chưa hiện theo ô vẫn có câu báo lỗi), và nếu nó là lỗi TRÙNG
 * (email / số điện thoại đã có) thì mã trả về là 409, còn lại là 400.
 */
public final class LoiForm {

    private final Map<String, String> loi = new LinkedHashMap<>();
    private boolean loiDauLaTrung;

    /** Lỗi định dạng / thiếu dữ liệu ở một ô. Ô đã có lỗi thì giữ lỗi cũ. */
    public LoiForm them(String truong, String thongBao) {
        return ghi(truong, thongBao, false);
    }

    /** Lỗi TRÙNG dữ liệu đã có (409). */
    public LoiForm trung(String truong, String thongBao) {
        return ghi(truong, thongBao, true);
    }

    /** Ghi lỗi nếu điều kiện đúng. */
    public LoiForm neu(boolean dieuKien, String truong, String thongBao) {
        return dieuKien ? them(truong, thongBao) : this;
    }

    private LoiForm ghi(String truong, String thongBao, boolean trung) {
        if (!loi.containsKey(truong)) {
            if (loi.isEmpty()) {
                loiDauLaTrung = trung;
            }
            loi.put(truong, thongBao);
        }
        return this;
    }

    public boolean co(String truong) {
        return loi.containsKey(truong);
    }

    public boolean coLoi() {
        return !loi.isEmpty();
    }

    public Map<String, String> theoTruong() {
        return new LinkedHashMap<>(loi);
    }

    /** Câu báo lỗi của ô đầu tiên bị lỗi. */
    public String loiDauTien() {
        return loi.isEmpty() ? null : loi.values().iterator().next();
    }

    /** Có lỗi thì ném ApiException (400, hoặc 409 nếu lỗi đầu tiên là trùng) kèm lỗi theo từng ô. */
    public void nemNeuCoLoi() {
        if (loi.isEmpty()) {
            return;
        }
        HttpStatus ma = loiDauLaTrung ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        throw new ApiException(ma, loiDauTien(), null, null, theoTruong());
    }
}
