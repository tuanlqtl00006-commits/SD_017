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

    @Autowired
    private com.footstyle.demo.repository.HoaDonRepository hoaDonRepo;

    /** Góp ý 21/11: hóa đơn chờ tại quầy (khách vãng lai, 0 đồng, chưa thanh toán) không vào Quản lý hóa đơn. */
    @Test
    void hoaDonChoTaiQuay_khongNamTrongDanhSachVaBoDem() {
        long truoc = service.demTheoTrangThai(null, null, null, null).get("tatCa");
        com.footstyle.demo.entity.HoaDon h = new com.footstyle.demo.entity.HoaDon();
        h.setMaHoaDon("HDCHO2111");
        h.setLoaiDon(com.footstyle.demo.entity.HoaDon.TAI_QUAY);
        h.setTrangThai(com.footstyle.demo.entity.HoaDon.CHO_XAC_NHAN);
        // Giống HoaDonChoService.tao(): mới tạo là 0 đồng, khách vãng lai
        h.setTongTien(java.math.BigDecimal.ZERO);
        h.setSoTienGiam(java.math.BigDecimal.ZERO);
        h.setPhiVanChuyen(java.math.BigDecimal.ZERO);
        h.setThanhTien(java.math.BigDecimal.ZERO);
        h.setNgayTao(java.time.LocalDateTime.now());
        h.setNgayCapNhat(java.time.LocalDateTime.now());
        h = hoaDonRepo.saveAndFlush(h);
        Integer id = h.getId();

        assertTrue(ds(null).stream().noneMatch(x -> x.getId().equals(id)), "tab Tất cả không có hóa đơn chờ");
        assertTrue(ds(HoaDonService.KENH_TAI_QUAY).stream().noneMatch(x -> x.getId().equals(id)), "tab Đơn tại quầy không có hóa đơn chờ");
        assertEquals(truoc, service.demTheoTrangThai(null, null, null, null).get("tatCa"), "bộ đếm không tăng");
        assertEquals(0L, service.demTheoTrangThai(null, null, null, null, HoaDonService.KENH_TAI_QUAY).get("1"), "tab Chờ xác nhận của tại quầy = 0");
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
