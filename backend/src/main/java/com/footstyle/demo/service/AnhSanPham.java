package com.footstyle.demo.service;

import com.footstyle.demo.dto.AnhTheoMauResponse;
import com.footstyle.demo.entity.HinhAnhSanPham;
import com.footstyle.demo.entity.SanPham;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Các phép tính về ảnh sản phẩm (bảng hinh_anh_san_pham), tách riêng để kiểm thử không cần CSDL.
 *
 * Một sản phẩm có 2 loại ảnh:
 *  - ảnh CHUNG (id_mau_sac = NULL): ảnh chính + ảnh phụ của sản phẩm (như trước đây);
 *  - ảnh THEO MÀU (id_mau_sac = id màu): "Ảnh sản phẩm màu Đen" áp dụng cho mọi biến thể màu Đen. Mỗi màu có 1 ảnh chính.
 * Ảnh không bao giờ bị xóa dòng: bỏ ảnh thì trang_thai = 0.
 */
public final class AnhSanPham {

    private AnhSanPham() {
    }

    private static boolean dangDung(HinhAnhSanPham h) {
        return h.getTrangThai() != null && h.getTrangThai() == HinhAnhSanPham.HOAT_DONG;
    }

    /** Ảnh chính xếp trước, sau đó theo id tăng dần (ảnh thêm trước đứng trước). */
    private static final Comparator<HinhAnhSanPham> THU_TU = Comparator
            .comparing((HinhAnhSanPham h) -> Boolean.TRUE.equals(h.getLaAnhChinh()) ? 0 : 1)
            .thenComparing(h -> h.getId() == null ? Integer.MAX_VALUE : h.getId());

    /** Ảnh chung đang dùng của sản phẩm (id_mau_sac = NULL), ảnh chính đứng đầu. */
    public static List<HinhAnhSanPham> anhChung(List<HinhAnhSanPham> tatCa) {
        List<HinhAnhSanPham> ds = new ArrayList<>();
        for (HinhAnhSanPham h : tatCa) {
            if (dangDung(h) && h.getIdMauSac() == null) ds.add(h);
        }
        ds.sort(THU_TU);
        return ds;
    }

    /** Ảnh theo màu đang dùng: {idMàu -> [url...]} (ảnh chính của màu đứng đầu), màu xếp theo id tăng dần. */
    public static Map<Integer, List<String>> anhTheoMau(List<HinhAnhSanPham> tatCa) {
        List<HinhAnhSanPham> ds = new ArrayList<>();
        for (HinhAnhSanPham h : tatCa) {
            if (dangDung(h) && h.getIdMauSac() != null) ds.add(h);
        }
        ds.sort(Comparator.comparing(HinhAnhSanPham::getIdMauSac).thenComparing(THU_TU));
        Map<Integer, List<String>> ketQua = new LinkedHashMap<>();
        for (HinhAnhSanPham h : ds) {
            ketQua.computeIfAbsent(h.getIdMauSac(), k -> new ArrayList<>()).add(h.getDuongDanHinhAnh());
        }
        return ketQua;
    }

    /** Dạng trả về cho giao diện: [{ idMauSac, anh: [...] }]. */
    public static List<AnhTheoMauResponse> anhTheoMauResponse(List<HinhAnhSanPham> tatCa) {
        List<AnhTheoMauResponse> ds = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> e : anhTheoMau(tatCa).entrySet()) {
            ds.add(new AnhTheoMauResponse(e.getKey(), e.getValue()));
        }
        return ds;
    }

    /** Ảnh đại diện để hiện ở danh sách: ảnh chính chung, không có thì ảnh chính của màu đầu tiên, không có nữa thì "". */
    public static String anhDaiDien(List<HinhAnhSanPham> tatCa) {
        List<HinhAnhSanPham> chung = anhChung(tatCa);
        if (!chung.isEmpty()) return chung.get(0).getDuongDanHinhAnh();
        for (List<String> ds : anhTheoMau(tatCa).values()) {
            if (!ds.isEmpty()) return ds.get(0);
        }
        return "";
    }

    /** Ảnh của một biến thể: ảnh chính của màu biến thể; màu chưa có ảnh thì lấy ảnh chính chung; không có thì "". */
    public static String anhCuaBienThe(List<HinhAnhSanPham> anhCuaSanPham, Integer idMauSac) {
        if (idMauSac != null) {
            List<String> theoMau = anhTheoMau(anhCuaSanPham).get(idMauSac);
            if (theoMau != null && !theoMau.isEmpty()) return theoMau.get(0);
        }
        List<HinhAnhSanPham> chung = anhChung(anhCuaSanPham);
        return chung.isEmpty() ? "" : chung.get(0).getDuongDanHinhAnh();
    }

    /**
     * Ảnh theo màu SAU KHI lưu: lấy ảnh đang dùng của sản phẩm, rồi áp danh sách gửi lên (màu nào có trong danhSachMoi thì
     * thay bằng danh sách đó; danh sách rỗng nghĩa là bỏ hết ảnh của màu, chỉ khi choPhepBoHet = true, ngược lại bỏ qua màu đó).
     */
    public static Map<Integer, List<String>> anhTheoMauSauKhiLuu(List<HinhAnhSanPham> hienCo, Map<Integer, List<String>> danhSachMoi, boolean choPhepBoHet) {
        Map<Integer, List<String>> ketQua = new LinkedHashMap<>(anhTheoMau(hienCo));
        danhSachMoi.forEach((idMau, ds) -> {
            if (!ds.isEmpty()) {
                ketQua.put(idMau, ds);
            } else if (choPhepBoHet) {
                ketQua.remove(idMau);
            }
        });
        return ketQua;
    }

    /** Các màu trong mauCan mà chưa có ảnh riêng nào (theo thứ tự mauCan). */
    public static List<Integer> mauChuaCoAnh(Collection<Integer> mauCan, Map<Integer, List<String>> anhTheoMau) {
        List<Integer> thieu = new ArrayList<>();
        for (Integer id : mauCan) {
            List<String> ds = anhTheoMau.get(id);
            if (ds == null || ds.isEmpty()) thieu.add(id);
        }
        return thieu;
    }

    /** Có ít nhất một màu đang có ảnh riêng không. */
    public static boolean coAnhTheoMau(Map<Integer, List<String>> anhTheoMau) {
        for (List<String> ds : anhTheoMau.values()) {
            if (!ds.isEmpty()) return true;
        }
        return false;
    }

    /**
     * Đồng bộ ảnh của MỘT nhóm (ảnh chung khi idMauSac = null, hoặc ảnh của một màu) với danh sách mong muốn.
     * Chỉ đụng tới các dòng cùng nhóm: dòng còn trong danh sách thì bật, dòng bị bỏ thì trang_thai = 0 (không xóa),
     * url mới thì thêm dòng. Đúng 1 ảnh (phần tử đầu tiên của danh sách) có la_anh_chinh = 1.
     * Trả về các dòng cần lưu (chưa lưu xuống CSDL).
     */
    public static List<HinhAnhSanPham> dongBo(SanPham sp, Integer idMauSac, List<HinhAnhSanPham> hienCo, List<String> anh) {
        List<HinhAnhSanPham> canLuu = new ArrayList<>();
        Set<String> daXuLy = new HashSet<>();
        String chinh = anh.isEmpty() ? null : anh.get(0);
        for (HinhAnhSanPham h : hienCo) {
            if (!Objects.equals(h.getIdMauSac(), idMauSac)) continue; // dòng của nhóm khác: giữ nguyên
            String url = h.getDuongDanHinhAnh();
            boolean giu = anh.contains(url) && daXuLy.add(url); // add trả false nếu url đã có dòng khác dùng
            h.setTrangThai(giu ? HinhAnhSanPham.HOAT_DONG : HinhAnhSanPham.NGUNG_HOAT_DONG);
            h.setLaAnhChinh(giu && url.equals(chinh));
            canLuu.add(h);
        }
        for (String url : anh) {
            if (daXuLy.add(url)) {
                HinhAnhSanPham moi = new HinhAnhSanPham();
                moi.setSanPham(sp);
                moi.setIdMauSac(idMauSac);
                moi.setDuongDanHinhAnh(url);
                moi.setLaAnhChinh(url.equals(chinh));
                moi.setTrangThai(HinhAnhSanPham.HOAT_DONG);
                canLuu.add(moi);
            }
        }
        return canLuu;
    }
}
