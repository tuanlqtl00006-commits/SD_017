package com.footstyle.demo.controller;

import com.footstyle.demo.security.NguoiDung;
import com.footstyle.demo.security.XacThuc;
import com.footstyle.demo.service.ChatNoiBoService;
import com.footstyle.demo.service.HoTroService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Chat nội bộ giữa nhân viên + số tin chưa đọc (menu / chuông thông báo). */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatNoiBoService noiBo;
    private final HoTroService hoTro;

    /** { hoTro: số cuộc chat khách cần xử lý, noiBo: số tin nội bộ chưa đọc } */
    @GetMapping("/chua-doc")
    public Map<String, Object> chuaDoc() {
        NguoiDung nd = XacThuc.nhanVien();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hoTro", hoTro.soCanXuLy(nd.id()));
        m.put("noiBo", noiBo.soChuaDoc(nd.id()));
        return m;
    }

    @GetMapping("/nhan-vien")
    public List<Map<String, Object>> nhanVien() {
        return noiBo.nhanVien();
    }

    @GetMapping("/noi-bo")
    public List<Map<String, Object>> danhSach() {
        return noiBo.danhSach();
    }

    @PostMapping("/noi-bo/rieng")
    public Map<String, Object> moRieng(@RequestBody(required = false) ChatNoiBoService.MoRiengRequest req) {
        return noiBo.moRieng(req);
    }

    @PostMapping("/noi-bo/nhom")
    public Map<String, Object> taoNhom(@RequestBody(required = false) ChatNoiBoService.TaoNhomRequest req) {
        return noiBo.taoNhom(req);
    }

    @GetMapping("/noi-bo/{id}")
    public Map<String, Object> xem(@PathVariable Integer id, @RequestParam(required = false) Integer sauId) {
        return noiBo.xem(id, sauId);
    }

    @PostMapping("/noi-bo/{id}/tin-nhan")
    public Map<String, Object> gui(@PathVariable Integer id, @RequestBody(required = false) ChatNoiBoService.NoiDungRequest req) {
        return noiBo.gui(id, req);
    }
}
