package com.footstyle.demo.service;

import com.footstyle.demo.entity.DotGiamGia;
import com.footstyle.demo.entity.DotGiamGiaChiTiet;
import com.footstyle.demo.repository.DotGiamGiaChiTietRepository;
import com.footstyle.demo.service.GiamGiaChongDot.CheDo;
import com.footstyle.demo.service.GiamGiaChongDot.Doan;
import com.footstyle.demo.service.GiamGiaChongDot.DotApDung;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mức giảm đang áp dụng cho từng biến thể khi nhiều đợt giảm giá chạy cùng ngày.
 * Cách tính khi trùng đợt đặt trong application.properties: app.giam-gia.che-do-chong-dot = MAX | TRUNG_BINH
 * (MAX: lấy mức cao nhất, mặc định; TRUNG_BINH: trung bình cộng các đợt).
 */
@Service
@RequiredArgsConstructor
public class GiamGiaApDungService {

    private final DotGiamGiaChiTietRepository chiTietRepository;

    @Value("${app.giam-gia.che-do-chong-dot:MAX}")
    private String cheDoCauHinh;

    public CheDo cheDo() {
        return CheDo.tuChuoi(cheDoCauHinh);
    }

    /** Chi tiết còn hiệu lực (đợt đang bật, dòng chi tiết đang bật) của một biến thể, đổi sang DotApDung. */
    private DotApDung toDotApDung(DotGiamGiaChiTiet ct) {
        DotGiamGia dot = ct.getDotGiamGia();
        if (dot == null || dot.getNgayBatDau() == null || dot.getNgayKetThuc() == null) return null;
        if (dot.getTrangThai() == null || dot.getTrangThai() != 1) return null;
        if (ct.getTrangThai() != null && ct.getTrangThai() != 1) return null;
        int pt = ct.getPhanTramGiamBienThe() == null ? 0 : ct.getPhanTramGiamBienThe();
        return new DotApDung(dot.getId(), dot.getNgayBatDau().toLocalDate(), dot.getNgayKetThuc().toLocalDate(), pt);
    }

    /** { idBienThe: % giảm } của các đợt đang diễn ra vào ngày được hỏi. Biến thể không giảm thì không có trong kết quả. */
    @Transactional(readOnly = true)
    public Map<Long, Double> phanTramTheoBienThe(LocalDate ngay) {
        Map<Long, List<DotApDung>> theoBienThe = new HashMap<>();
        for (DotGiamGiaChiTiet ct : chiTietRepository.findAll()) {
            DotApDung d = toDotApDung(ct);
            if (d != null) theoBienThe.computeIfAbsent(ct.getIdSanPhamChiTiet(), k -> new ArrayList<>()).add(d);
        }
        CheDo cheDo = cheDo();
        Map<Long, Double> ketQua = new HashMap<>();
        for (Map.Entry<Long, List<DotApDung>> e : theoBienThe.entrySet()) {
            double pt = GiamGiaChongDot.phanTramTaiNgay(e.getValue(), ngay, cheDo);
            if (pt > 0) ketQua.put(e.getKey(), pt);
        }
        return ketQua;
    }

    /** Lịch giảm giá của một biến thể theo từng đoạn ngày (kể cả đợt chưa bắt đầu). */
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
