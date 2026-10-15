package com.footstyle.demo.controller;

import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.service.GiamGiaApDungService;
import com.footstyle.demo.service.GiamGiaChongDot.Doan;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Giảm giá thực tế của biến thể khi có nhiều đợt chạy chồng thời gian (xem GiamGiaChongDot). */
@RestController
@RequestMapping("/api/giam-gia")
@RequiredArgsConstructor
public class GiamGiaApDungController {

    private final GiamGiaApDungService service;

    /**
     * % giảm đang áp dụng của mọi biến thể tại một thời điểm (mặc định: bây giờ):
     * { cheDo, thoiDiem, phanTram: { idBienThe: % } }.
     * thoiDiem = yyyy-MM-ddTHH:mm[:ss]; hoặc ngay = yyyy-MM-dd (lấy 12:00 trưa hôm đó).
     */
    @GetMapping("/ap-dung")
    public Map<String, Object> apDung(@RequestParam(required = false) String thoiDiem,
                                      @RequestParam(required = false) String ngay) {
        LocalDateTime t = LocalDateTime.now();
        try {
            if (thoiDiem != null && !thoiDiem.isBlank()) {
                t = LocalDateTime.parse(thoiDiem.trim());
            } else if (ngay != null && !ngay.isBlank()) {
                t = LocalDate.parse(ngay.trim()).atTime(12, 0);
            }
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Thời điểm không hợp lệ, định dạng yyyy-MM-ddTHH:mm (hoặc ngay=yyyy-MM-dd).");
        }
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("cheDo", service.cheDo().name());
        kq.put("thoiDiem", t.toString());
        kq.put("phanTram", service.phanTramTheoBienThe(t));
        return kq;
    }

    /** Lịch giảm giá của một biến thể theo từng đoạn thời gian. */
    @GetMapping("/bien-the/{id}/lich")
    public List<Map<String, Object>> lich(@PathVariable Long id) {
        List<Map<String, Object>> kq = new ArrayList<>();
        for (Doan doan : service.lichBienThe(id)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tuLuc", doan.tuLuc().toString());
            m.put("denLuc", doan.denLuc().toString());
            m.put("phanTram", doan.phanTram());
            m.put("trungDot", doan.trungDot());
            m.put("idDots", doan.idDots());
            kq.add(m);
        }
        return kq;
    }
}
