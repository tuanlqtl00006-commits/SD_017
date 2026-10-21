package com.footstyle.demo.controller;

import com.footstyle.demo.service.HoTroService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Chat hỗ trợ khách - phía NHÂN VIÊN: xem danh sách, nhận hỗ trợ, trả lời, kết thúc. */
@RestController
@RequestMapping("/api/ho-tro")
@RequiredArgsConstructor
public class HoTroController {

    private final HoTroService service;

    @GetMapping
    public Map<String, Object> danhSach(@RequestParam(required = false) String loc) {
        return service.danhSach(loc);
    }

    @GetMapping("/{id:\\d+}")
    public Map<String, Object> xem(@PathVariable Integer id, @RequestParam(required = false) Integer sauId) {
        return service.xem(id, sauId);
    }

    @PostMapping("/{id:\\d+}/nhan")
    public Map<String, Object> nhan(@PathVariable Integer id) {
        return service.nhan(id);
    }

    @PostMapping("/{id:\\d+}/tin-nhan")
    public Map<String, Object> gui(@PathVariable Integer id, @RequestBody(required = false) HoTroService.NoiDungRequest req) {
        return service.gui(id, req);
    }

    @PostMapping("/{id:\\d+}/dong")
    public Map<String, Object> dong(@PathVariable Integer id) {
        return service.dong(id);
    }
}
