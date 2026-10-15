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

    /** Đổi cách tính khi nhiều đợt chồng nhau ngay lúc chạy (MAX = lấy mức cao nhất, TRUNG_BINH = trung bình cộng). */
    public CheDo doiCheDo(String cheDoMoi) {
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

    @Transactional(readOnly = true)
    public Map<Long, Double> phanTramTheoBienThe(java.time.LocalDate ngay) {
        return phanTramTheoBienThe(ngay.atTime(12, 0));
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
