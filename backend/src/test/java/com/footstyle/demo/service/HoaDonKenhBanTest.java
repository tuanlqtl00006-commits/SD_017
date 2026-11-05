package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.dto.HoaDonRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý hóa đơn chia 2 tab Đơn tại quầy / Đơn online (review 05/11). Đơn bán quầy có bật "Giao hàng" lưu loại giao hàng
 * (giống đơn online) nên kênh bán lấy theo dòng lịch sử lúc tạo đơn. Chạy trên CSDL thật, chỉ đọc.
 */
@SpringBootTest
@Transactional
class HoaDonKenhBanTest {

    @Autowired
    private HoaDonService service;

    private List<HoaDonRequest> ds(String kenh) {
        return service.getDanhSachCoLoc(null, null, null, null, null, kenh, PageRequest.of(0, 2000)).getContent();
    }

    @Test
    void moiDonThuocDungMotTab_vaTenLoaiDonKhopTab() {
        List<HoaDonRequest> taiQuay = ds(HoaDonService.KENH_TAI_QUAY);
        List<HoaDonRequest> online = ds(HoaDonService.KENH_ONLINE);
        List<HoaDonRequest> tatCa = ds(null);

        assertEquals(tatCa.size(), taiQuay.size() + online.size(), "tổng 2 tab = tất cả");
        Set<Integer> idQuay = new HashSet<>();
        taiQuay.forEach(h -> idQuay.add(h.getId()));
        assertTrue(online.stream().noneMatch(h -> idQuay.contains(h.getId())), "không đơn nào nằm ở cả 2 tab");
        assertTrue(taiQuay.stream().allMatch(h -> "Tại quầy".equals(h.getLoaiDon())), "tab tại quầy: mọi đơn ghi Tại quầy");
        assertTrue(online.stream().allMatch(h -> "Trực tuyến".equals(h.getLoaiDon())), "tab online: mọi đơn ghi Trực tuyến");
    }

    @Test
    void boDemTheoTab_khopDanhSach() {
        Map<String, Long> demQuay = service.demTheoTrangThai(null, null, null, null, HoaDonService.KENH_TAI_QUAY);
        Map<String, Long> demOnl = service.demTheoTrangThai(null, null, null, null, HoaDonService.KENH_ONLINE);
        assertEquals((long) ds(HoaDonService.KENH_TAI_QUAY).size(), demQuay.get("tatCa"));
        assertEquals((long) ds(HoaDonService.KENH_ONLINE).size(), demOnl.get("tatCa"));
        assertEquals(service.demTheoTrangThai(null, null, null, null).get("tatCa"), demQuay.get("tatCa") + demOnl.get("tatCa"));
    }
}
