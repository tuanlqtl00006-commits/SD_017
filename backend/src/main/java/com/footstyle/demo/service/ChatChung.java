package com.footstyle.demo.service;

import com.footstyle.demo.entity.ThanhVienHoiThoai;
import com.footstyle.demo.entity.TinNhan;
import com.footstyle.demo.exception.LoiForm;
import com.footstyle.demo.repository.TinNhanRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Phần dùng chung của chat hỗ trợ khách và chat nội bộ: kiểm tra nội dung, đọc tin, đổi tin sang dữ liệu trả về. */
final class ChatChung {

    private ChatChung() {
    }

    /** Nội dung tin nhắn đã bỏ khoảng trắng đầu / cuối; rỗng hoặc quá dài thì báo lỗi ở ô "noiDung". */
    static String noiDung(String s) {
        String nd = s == null ? "" : s.strip();
        LoiForm loi = new LoiForm();
        loi.neu(nd.isEmpty(), "noiDung", "Hãy nhập nội dung tin nhắn.");
        loi.neu(nd.length() > TinNhan.DO_DAI_TOI_DA, "noiDung", "Tin nhắn tối đa " + TinNhan.DO_DAI_TOI_DA + " ký tự (hiện có " + nd.length() + ").");
        loi.nemNeuCoLoi();
        return nd;
    }

    /** Lần mở đầu (sauId trống / 0): 100 tin gần nhất; các lần hỏi lại: chỉ tin mới hơn sauId. Luôn xếp cũ -> mới. */
    static List<TinNhan> tin(TinNhanRepository repo, Integer idHoiThoai, Integer sauId) {
        if (sauId == null || sauId <= 0) {
            List<TinNhan> ds = new ArrayList<>(repo.findTop100ByIdHoiThoaiOrderByIdDesc(idHoiThoai));
            Collections.reverse(ds);
            return ds;
        }
        return repo.findTop200ByIdHoiThoaiAndIdGreaterThanOrderByIdAsc(idHoiThoai, sauId);
    }

    /** Tin nhắn trả về. `choKhach` = true: không trả id nhân viên (khách chỉ cần tên người gửi). */
    static Map<String, Object> dto(TinNhan t, boolean choKhach) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("loaiNguoiGui", t.getLoaiNguoiGui());
        if (!choKhach) m.put("idNhanVien", t.getIdNhanVien());
        m.put("tenNguoiGui", t.getTenNguoiGui());
        m.put("noiDung", t.getNoiDung());
        m.put("thoiGian", t.getThoiGian());
        return m;
    }

    static List<Map<String, Object>> dto(List<TinNhan> ds, boolean choKhach) {
        List<Map<String, Object>> kq = new ArrayList<>();
        for (TinNhan t : ds) kq.add(dto(t, choKhach));
        return kq;
    }

    /** Tin cuối (xem trước trong danh sách hội thoại): cắt còn 80 ký tự. */
    static Map<String, Object> tinCuoi(TinNhan t) {
        if (t == null) return null;
        Map<String, Object> m = dto(t, false);
        String nd = t.getNoiDung() == null ? "" : t.getNoiDung().replaceAll("\\s+", " ");
        m.put("noiDung", nd.length() > 80 ? nd.substring(0, 80) + "…" : nd);
        return m;
    }

    /** Đánh dấu đã đọc tới tin `idTin` (chỉ tiến, không lùi). Trả về true nếu có đổi. */
    static boolean daDocToi(ThanhVienHoiThoai tv, Integer idTin) {
        if (idTin == null) return false;
        int cu = tv.getIdTinNhanDaDoc() == null ? 0 : tv.getIdTinNhanDaDoc();
        if (idTin <= cu) return false;
        tv.setIdTinNhanDaDoc(idTin);
        return true;
    }
}
