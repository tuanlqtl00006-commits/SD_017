package com.footstyle.demo.service;

import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import com.footstyle.demo.repository.DotGiamGiaChiTietRepository;
import com.footstyle.demo.service.GiamGiaChongDot.CheDo;
import com.footstyle.demo.service.GiamGiaChongDot.Doan;
import com.footstyle.demo.service.GiamGiaChongDot.DotApDung;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mức giảm đang áp dụng cho từng biến thể khi nhiều đợt giảm giá chạy chồng thời gian nhau (tính tới từng giờ / phút).
 * Cách tính khi trùng đợt đặt trong application.properties: app.giam-gia.che-do-chong-dot = MAX | CONG_DON | TRUNG_BINH
 * (MAX: lấy mức cao nhất, mặc định; CONG_DON: cộng dồn các đợt, tối đa 100%; TRUNG_BINH: trung bình cộng các đợt).
 */
@Service
@RequiredArgsConstructor
public class GiamGiaApDungService {

    private final DotGiamGiaChiTietRepository chiTietRepository;

    @Value("${app.giam-gia.che-do-chong-dot:MAX}")
    private volatile String cheDoCauHinh;

    public CheDo cheDo() {
        return CheDo.tuChuoi(cheDoCauHinh);
    }

    public CheDo datCheDo(String cheDoMoi) {
        CheDo cd = CheDo.tuChuoi(cheDoMoi);
        this.cheDoCauHinh = cd.name();
        return cd;
    }

    /** Chi tiết còn hiệu lực (đợt đang bật, dòng chi tiết đang bật) của một biến thể, đổi sang DotApDung. */
    private DotApDung toDotApDung(DotGiamGiaChiTiet ct) {
        DotGiamGia dot = ct.getDotGiamGia();
        if (dot == null || dot.getNgayBatDau() == null || dot.getNgayKetThuc() == null) return null;
        if (dot.getTrangThai() == null || dot.getTrangThai() != 1) return null;
        if (ct.getTrangThai() != null && ct.getTrangThai() != 1) return null;
        int pt = ct.getPhanTramGiamBienThe() == null ? 0 : ct.getPhanTramGiamBienThe();
        return new DotApDung(dot.getId(), dot.getNgayBatDau(), dot.getNgayKetThuc(), pt);
    }

    /** { idBienThe: % giảm } của các đợt đang diễn ra vào thời điểm được hỏi. Biến thể không giảm thì không có trong kết quả. */
    @Transactional(readOnly = true)
    public Map<Long, Double> phanTramTheoBienThe(LocalDateTime thoiDiem) {
        Map<Long, List<DotApDung>> theoBienThe = new HashMap<>();
        for (DotGiamGiaChiTiet ct : chiTietRepository.findAll()) {
            DotApDung d = toDotApDung(ct);
            if (d != null) theoBienThe.computeIfAbsent(ct.getIdSanPhamChiTiet(), k -> new ArrayList<>()).add(d);
        }
        CheDo cheDo = cheDo();
        Map<Long, Double> ketQua = new HashMap<>();
        for (Map.Entry<Long, List<DotApDung>> e : theoBienThe.entrySet()) {
            double pt = GiamGiaChongDot.phanTramTaiThoiDiem(e.getValue(), thoiDiem, cheDo);
            if (pt > 0) ketQua.put(e.getKey(), pt);
        }
        return ketQua;
    }

    /**
     * Các mức % có thể đã áp cho một biến thể vào các thời điểm cho trước — để tìm lại GIÁ GỐC lúc bán của dòng hóa đơn cũ
     * (bảng hoa_don_chi_tiet chỉ lưu đơn giá, không lưu giá gốc; không đổi SQL). Thứ tự: mức gộp của các đợt ĐANG BẬT tại từng
     * thời điểm trước (đúng cách tính lúc bán), sau đó % riêng của mọi đợt có khoảng ngày chứa thời điểm đó, kể cả đợt / dòng
     * đã bị tắt sau này. Không trùng lặp, chỉ mức > 0.
     */
    @Transactional(readOnly = true)
    public List<Double> mucGiamCoThe(Long idBienThe, List<LocalDateTime> thoiDiems) {
        List<Double> kq = new ArrayList<>();
        if (idBienThe == null) return kq;
        List<DotGiamGiaChiTiet> cts = chiTietRepository.findByIdSanPhamChiTiet(idBienThe);
        List<DotApDung> dangBat = new ArrayList<>();
        for (DotGiamGiaChiTiet ct : cts) {
            DotApDung d = toDotApDung(ct);
            if (d != null) dangBat.add(d);
        }
        for (LocalDateTime t : thoiDiems) {
            if (t == null) continue;
            double gop = GiamGiaChongDot.phanTramTaiThoiDiem(dangBat, t, cheDo());
            if (gop > 0 && !kq.contains(gop)) kq.add(gop);
        }
        for (LocalDateTime t : thoiDiems) {
            if (t == null) continue;
            for (DotGiamGiaChiTiet ct : cts) {
                DotGiamGia dot = ct.getDotGiamGia();
                if (dot == null || dot.getNgayBatDau() == null || dot.getNgayKetThuc() == null) continue;
                if (t.isBefore(dot.getNgayBatDau()) || t.isAfter(dot.getNgayKetThuc())) continue;
                double pt = ct.getPhanTramGiamBienThe() == null ? 0 : ct.getPhanTramGiamBienThe();
                if (pt > 0 && !kq.contains(pt)) kq.add(pt);
            }
        }
        return kq;
    }

    /** Lịch giảm giá của một biến thể theo từng đoạn thời gian (kể cả đợt chưa bắt đầu). */
    @Transactional(readOnly = true)
    public List<Doan> lichBienThe(Long idBienThe) {
        List<DotApDung> dots = new ArrayList<>();
        for (DotGiamGiaChiTiet ct : chiTietRepository.findByIdSanPhamChiTiet(idBienThe)) {
            DotApDung d = toDotApDung(ct);
            if (d != null) dots.add(d);
        }
        return GiamGiaChongDot.lich(dots, cheDo());
    }
}
