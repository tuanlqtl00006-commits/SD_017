package com.footstyle.demo.controller;

import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.service.GiamGiaApDungService;
import com.footstyle.demo.service.GiamGiaChongDot.Doan;
import java.time.LocalDate;
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

/** Giảm giá thực tế của biến thể khi có nhiều đợt chạy chồng ngày (xem GiamGiaChongDot). */
@RestController
@RequestMapping("/api/giam-gia")
@RequiredArgsConstructor
public class GiamGiaApDungController {

    private final GiamGiaApDungService service;

    /** % giảm đang áp dụng của mọi biến thể vào một ngày (mặc định hôm nay): { cheDo, ngay, phanTram: { idBienThe: % } }. */
    @GetMapping("/ap-dung")
    public Map<String, Object> apDung(@RequestParam(required = false) String ngay) {
        LocalDate d = LocalDate.now();
        if (ngay != null && !ngay.isBlank()) {
            try {
                d = LocalDate.parse(ngay.trim());
            } catch (DateTimeParseException e) {
                throw ApiException.badRequest("Ngày không hợp lệ, định dạng yyyy-MM-dd.");
            }
        }
        Map<String, Object> kq = new LinkedHashMap<>();
        kq.put("cheDo", service.cheDo().name());
        kq.put("ngay", d.toString());
        kq.put("phanTram", service.phanTramTheoBienThe(d));
        return kq;
    }

    /** Lịch giảm giá của một biến thể theo từng đoạn ngày. */
    @GetMapping("/bien-the/{id}/lich")
    public List<Map<String, Object>> lich(@PathVariable Long id) {
        List<Map<String, Object>> kq = new ArrayList<>();
        for (Doan doan : service.lichBienThe(id)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tuNgay", doan.tuNgay().toString());
            m.put("denNgay", doan.denNgay().toString());
            m.put("phanTram", doan.phanTram());
            m.put("trungDot", doan.trungDot());
            m.put("idDots", doan.idDots());
            kq.add(m);
        }
        return kq;
    }
}
