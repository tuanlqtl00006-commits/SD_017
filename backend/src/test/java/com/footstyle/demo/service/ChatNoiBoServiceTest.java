package com.footstyle.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.footstyle.demo.entity.HoiThoai;
import com.footstyle.demo.entity.TinNhan;
import com.footstyle.demo.entity.VaiTro;
import com.footstyle.demo.exception.ApiException;
import com.footstyle.demo.security.XacThuc;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ChatNoiBoServiceTest {

    private final ChatMau c = new ChatMau();
    private final DongHoChinh dongHo = new DongHoChinh(CaLamMau.HOM_NAY.atTime(9, 0));
    private final ChatNoiBoService service;

    ChatNoiBoServiceTest() {
        service = new ChatNoiBoService(c.hoiThoaiRepo(), c.thanhVienRepo(), c.tinNhanRepo(), c.nhanVienRepo());
        service.datDongHo(dongHo);
    }

    @AfterEach
    void don() {
        XacThuc.xoa();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> tin(Map<String, Object> m) {
        return (List<Map<String, Object>>) m.get("tinNhan");
    }

    private Integer nhomChung() {
        return c.hoiThoais.stream().filter(h -> HoiThoai.KHOA_NHOM_CHUNG.equals(h.getKhoaRieng())).findFirst().orElseThrow().getId();
    }

    private void gui(Integer id, String noiDung) {
        dongHo.tien(60);
        service.gui(id, new ChatNoiBoService.NoiDungRequest(noiDung));
    }

    @Test
    void phaiDangNhap() {
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ApiException.class, service::danhSach).getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ApiException.class, service::nhanVien).getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ApiException.class, () -> service.xem(1, null)).getStatus());
    }

    @Test
    void nhomChung_tuTaoMotLan_moiNguoiTuVao_tinCuCoiNhuDaDoc() {
        ChatMau.dangNhap(c.m.bao);
        List<Map<String, Object>> ds = service.danhSach();
        assertEquals(1, ds.size());
        assertEquals(ChatNoiBoService.TEN_NHOM_CHUNG, ds.get(0).get("tieuDe"));
        assertEquals(true, ds.get(0).get("nhomChung"));
        Integer nhom = nhomChung();
        gui(nhom, "Chào cả nhà");
        gui(nhom, "Hôm nay ai ca tối?");

        ChatMau.dangNhap(c.m.anh); // vào sau: không bị 2 tin cũ tính là chưa đọc
        assertEquals(0L, service.danhSach().get(0).get("chuaDoc"));
        assertEquals(1, c.hoiThoais.size(), "không tạo nhóm chung thứ 2");
        assertEquals(2, tin(service.xem(nhom, null)).size(), "vẫn xem được tin cũ");
        gui(nhom, "Em ca tối ạ");

        ChatMau.dangNhap(c.m.bao);
        assertEquals(1L, service.soChuaDoc(c.m.bao.getId()));
        assertEquals(1L, service.danhSach().get(0).get("chuaDoc"));
        assertEquals(2, ((List<?>) service.danhSach().get(0).get("thanhVien")).size(), "2 thành viên: Bảo + Anh");
        Map<String, Object> xem = service.xem(nhom, null);
        assertEquals(3, tin(xem).size());
        assertEquals(0L, service.soChuaDoc(c.m.bao.getId()));
        // Tin mình gửi không tính chưa đọc với chính mình
        gui(nhom, "Ok em");
        assertEquals(0L, service.soChuaDoc(c.m.bao.getId()));
        // Quản lý cũng là thành viên nhóm chung khi mở chat (tin trước đó coi như đã đọc)
        ChatMau.dangNhap(c.m.quanLy);
        assertEquals(0L, service.soChuaDoc(c.m.quanLy.getId()));
    }

    @Test
    void chatRieng_moiCapMotCuoc_tieuDeLaTenNguoiKia() {
        ChatMau.dangNhap(c.m.bao);
        Map<String, Object> r = service.moRieng(new ChatNoiBoService.MoRiengRequest(c.m.anh.getId()));
        assertEquals(HoiThoai.NOI_BO_RIENG, r.get("loai"));
        assertEquals("Phạm Minh Anh", r.get("tieuDe"));
        assertEquals("R:3-4", c.hoiThoais.stream().filter(h -> h.getId().equals(r.get("id"))).findFirst().orElseThrow().getKhoaRieng());
        gui((Integer) r.get("id"), "Anh ơi mai đổi ca được không?");

        ChatMau.dangNhap(c.m.anh);
        Map<String, Object> r2 = service.moRieng(new ChatNoiBoService.MoRiengRequest(c.m.bao.getId()));
        assertEquals(r.get("id"), r2.get("id"), "mở từ phía bên kia vẫn là cuộc cũ");
        assertEquals("Lê Quốc Bảo", r2.get("tieuDe"));
        assertEquals(1, tin(r2).size());
        assertEquals(TinNhan.NHAN_VIEN, tin(r2).get(0).get("loaiNguoiGui"));
        assertEquals(c.m.bao.getId(), tin(r2).get(0).get("idNhanVien"));

        // Người thứ 3 không xem / gửi được
        ChatMau.dangNhap(c.m.quanLy);
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ApiException.class, () -> service.xem((Integer) r.get("id"), null)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ApiException.class, () -> service.gui((Integer) r.get("id"), new ChatNoiBoService.NoiDungRequest("x"))).getStatus());
    }

    @Test
    void chatRieng_kiemTraNguoiNhan() {
        ChatMau.dangNhap(c.m.bao);
        assertEquals("Hãy chọn nhân viên.", assertThrows(ApiException.class, () -> service.moRieng(null)).getMessage());
        assertEquals("Không thể tự chat với chính mình.",
                assertThrows(ApiException.class, () -> service.moRieng(new ChatNoiBoService.MoRiengRequest(c.m.bao.getId()))).getMessage());
        ApiException e = assertThrows(ApiException.class, () -> service.moRieng(new ChatNoiBoService.MoRiengRequest(c.m.nghi.getId())));
        assertEquals("Nhân viên không tồn tại hoặc đã nghỉ.", e.getLoiTheoTruong().get("idNhanVien"));
        assertThrows(ApiException.class, () -> service.moRieng(new ChatNoiBoService.MoRiengRequest(999)));
        assertTrue(c.hoiThoais.isEmpty());
    }

    @Test
    void danhSachNhanVien_boMinhVaNguoiDaNghi() {
        ChatMau.dangNhap(c.m.bao);
        List<Object> ten = service.nhanVien().stream().map(m -> m.get("hoTen")).toList();
        assertEquals(List.of("Nguyễn Hoàng Long", "Phạm Minh Anh"), ten);
        assertEquals(VaiTro.TEN_QUAN_LY, service.nhanVien().get(0).get("vaiTro"));
        assertEquals(true, service.nhanVien().get(0).get("dangLam"));
    }

    @Test
    void taoNhom_kiemTraTungO_vaThanhVien() {
        ChatMau.dangNhap(c.m.bao);
        ApiException e = assertThrows(ApiException.class, () -> service.taoNhom(new ChatNoiBoService.TaoNhomRequest("  ", List.of())));
        assertEquals("Hãy đặt tên nhóm.", e.getLoiTheoTruong().get("tieuDe"));
        assertEquals("Hãy chọn ít nhất 1 nhân viên khác.", e.getLoiTheoTruong().get("idNhanViens"));
        e = assertThrows(ApiException.class, () -> service.taoNhom(new ChatNoiBoService.TaoNhomRequest("x".repeat(101), List.of(c.m.bao.getId()))));
        assertEquals("Tên nhóm tối đa 100 ký tự.", e.getLoiTheoTruong().get("tieuDe"));
        assertEquals("Hãy chọn ít nhất 1 nhân viên khác.", e.getLoiTheoTruong().get("idNhanViens"), "chọn mỗi mình thì không tính");
        e = assertThrows(ApiException.class, () -> service.taoNhom(new ChatNoiBoService.TaoNhomRequest("Nhóm", List.of(c.m.anh.getId(), c.m.nghi.getId()))));
        assertTrue(e.getLoiTheoTruong().get("idNhanViens").contains("đã nghỉ"));
        assertTrue(c.hoiThoais.isEmpty());

        Map<String, Object> r = service.taoNhom(new ChatNoiBoService.TaoNhomRequest("  Ca   tối  ", Arrays.asList(c.m.anh.getId(), null, c.m.anh.getId(), c.m.bao.getId())));
        assertEquals("Ca tối", r.get("tieuDe"));
        assertEquals(HoiThoai.NOI_BO_NHOM, r.get("loai"));
        assertEquals(2, ((List<?>) r.get("thanhVien")).size());
        assertEquals("Lê Quốc Bảo đã tạo nhóm \"Ca tối\".", tin(r).get(0).get("noiDung"));
        assertEquals(0L, service.soChuaDoc(c.m.bao.getId()), "người tạo đã xem");
        ChatMau.dangNhap(c.m.anh);
        assertEquals(1L, service.soChuaDoc(c.m.anh.getId()), "người được thêm thấy thông báo tạo nhóm");
        // Nhóm vừa có tin mới nhất xếp ngay sau nhóm chung
        List<Map<String, Object>> ds = service.danhSach();
        assertEquals(List.of(ChatNoiBoService.TEN_NHOM_CHUNG, "Ca tối"), ds.stream().map(m -> m.get("tieuDe")).toList());
    }

    @Test
    void xem_sauId_chiLayTinMoi_vaKiemTraNoiDung() {
        ChatMau.dangNhap(c.m.bao);
        service.danhSach();
        Integer nhom = nhomChung();
        gui(nhom, "1");
        gui(nhom, "2");
        Integer id1 = (Integer) tin(service.xem(nhom, null)).get(0).get("id");
        List<Map<String, Object>> moi = tin(service.xem(nhom, id1));
        assertEquals(1, moi.size());
        assertEquals("2", moi.get(0).get("noiDung"));
        ApiException e = assertThrows(ApiException.class, () -> service.gui(nhom, new ChatNoiBoService.NoiDungRequest("x".repeat(1001))));
        assertTrue(e.getLoiTheoTruong().containsKey("noiDung"));
        assertThrows(ApiException.class, () -> service.gui(nhom, null));
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, () -> service.xem(999, null)).getStatus());
    }

    @Test
    void khongMoDuocChatHoTroKhachQuaChatNoiBo() {
        HoiThoai hoTro = new HoiThoai();
        hoTro.setLoai(HoiThoai.HO_TRO_KHACH);
        hoTro.setNgayTao(dongHo.instant().atZone(CaLamMau.VN).toLocalDateTime());
        hoTro.setNgayCapNhat(hoTro.getNgayTao());
        c.hoiThoaiRepo().save(hoTro);
        ChatMau.dangNhap(c.m.bao);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class, () -> service.xem(hoTro.getId(), null)).getStatus());
        assertFalse(service.danhSach().stream().anyMatch(m -> m.get("id").equals(hoTro.getId())));
    }
}
