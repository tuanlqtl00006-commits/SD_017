package com.footstyle.demo.service;

import com.footstyle.demo.entity.HoaDon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Luật chuyển trạng thái hóa đơn - viết MỘT chỗ duy nhất: backend dùng để kiểm tra khi đổi trạng thái, còn giao diện
 * chỉ hiện đúng những nút mà API chi tiết hóa đơn trả về trong danh sách "chuyenDuoc" (không tự suy luận lại).
 *
 * <pre>
 * Đơn giao hàng : Chờ xác nhận -> Đã xác nhận -> Chờ giao hàng -> Đang giao hàng -> Đã giao hàng -> Đã hoàn thành
 *                 - Hủy đơn được khi chưa giao cho đơn vị vận chuyển (3 bước đầu), bắt buộc ghi lý do.
 *                 - Đang giao hàng có thể báo "Giao hàng thất bại" (bắt buộc ghi lý do: khách không nghe máy, từ chối nhận,
 *                   sai địa chỉ...). Từ "Giao hàng thất bại" thì giao lại (về Đang giao hàng) hoặc hủy đơn (hàng về kho nên cộng lại tồn).
 * Đơn tại quầy  : Chờ xác nhận -> Đã hoàn thành (thanh toán xong là xong); còn chờ thì hủy được.
 * Đã hủy, Đã hoàn thành, Hoàn tiền là trạng thái CUỐI: không đổi được nữa.
 * </pre>
 */
final class HoaDonLuongTrangThai {

    /** Tên hiển thị của từng trạng thái; cũng là chữ ghi vào cột lich_su_hoa_don.hanh_dong. */
    private static final Map<Integer, String> TEN;

    static {
        Map<Integer, String> ten = new LinkedHashMap<>();
        ten.put(HoaDon.DA_HUY, "Đã hủy");
        ten.put(HoaDon.CHO_XAC_NHAN, "Chờ xác nhận");
        ten.put(HoaDon.DA_XAC_NHAN, "Đã xác nhận");
        ten.put(HoaDon.CHO_LAY_HANG, "Chờ giao hàng");
        ten.put(HoaDon.DANG_GIAO, "Đang giao hàng");
        ten.put(HoaDon.DA_GIAO, "Đã giao hàng");
        ten.put(HoaDon.HOAN_THANH, "Đã hoàn thành");
        ten.put(HoaDon.HOAN_TIEN, "Hoàn tiền");
        ten.put(HoaDon.GIAO_THAT_BAI, "Giao hàng thất bại");
        TEN = Collections.unmodifiableMap(ten);
    }

    private HoaDonLuongTrangThai() {
    }

    static boolean hopLe(Integer trangThai) {
        return trangThai != null && TEN.containsKey(trangThai);
    }

    /** Tên trạng thái; mã lạ thì "Không xác định". */
    static String ten(Integer trangThai) {
        return TEN.getOrDefault(trangThai, "Không xác định");
    }

    /** Mọi (mã, tên) đang có - để dò ngược từ chữ trong lịch sử sang mã. */
    static Map<Integer, String> tatCaTen() {
        return TEN;
    }

    /** Trạng thái cuối: Đã hủy, Đã hoàn thành, Hoàn tiền - hóa đơn đã vào đây thì không đổi trạng thái nữa. */
    static boolean laTrangThaiCuoi(int trangThai) {
        return trangThai == HoaDon.DA_HUY || trangThai == HoaDon.HOAN_THANH || trangThai == HoaDon.HOAN_TIEN;
    }

    /** Chuyển sang các trạng thái này thì bắt buộc nhập lý do: hủy đơn và giao hàng thất bại. */
    static boolean batBuocLyDo(int trangThaiMoi) {
        return trangThaiMoi == HoaDon.DA_HUY || trangThaiMoi == HoaDon.GIAO_THAT_BAI;
    }

    /**
     * Các trạng thái mà hóa đơn đang ở `hienTai` được chuyển sang, theo thứ tự nút trên giao diện
     * (bước đi tiếp trước, hủy / thất bại sau). Trạng thái cuối thì rỗng.
     */
    static List<Integer> chuyenDuoc(Integer loaiDon, int hienTai) {
        List<Integer> ds = new ArrayList<>();
        if (laTrangThaiCuoi(hienTai)) {
            return ds;
        }
        if (!Integer.valueOf(HoaDon.GIAO_HANG).equals(loaiDon)) {
            // Đơn tại quầy: thanh toán xong là hoàn thành; chưa giao cho ai nên còn "chờ" thì hủy được. Không có Giao hàng thất bại.
            if (hienTai >= HoaDon.CHO_XAC_NHAN && hienTai <= HoaDon.DA_GIAO) {
                ds.add(HoaDon.HOAN_THANH);
            }
            if (hienTai >= HoaDon.CHO_XAC_NHAN && hienTai <= HoaDon.CHO_LAY_HANG) {
                ds.add(HoaDon.DA_HUY);
            }
            return ds;
        }
        switch (hienTai) {
            case HoaDon.CHO_XAC_NHAN -> {
                ds.add(HoaDon.DA_XAC_NHAN);
                ds.add(HoaDon.DA_HUY);
            }
            case HoaDon.DA_XAC_NHAN -> {
                ds.add(HoaDon.CHO_LAY_HANG);
                ds.add(HoaDon.DA_HUY);
            }
            case HoaDon.CHO_LAY_HANG -> {
                ds.add(HoaDon.DANG_GIAO);
                ds.add(HoaDon.DA_HUY);
            }
            case HoaDon.DANG_GIAO -> {
                ds.add(HoaDon.DA_GIAO);
                ds.add(HoaDon.GIAO_THAT_BAI);
            }
            case HoaDon.DA_GIAO -> ds.add(HoaDon.HOAN_THANH);
            case HoaDon.GIAO_THAT_BAI -> {
                ds.add(HoaDon.DANG_GIAO); // giao lại
                ds.add(HoaDon.DA_HUY);
            }
            default -> {
                // mã lạ (dữ liệu nhập tay): không cho đổi
            }
        }
        return ds;
    }

    /** Câu báo lỗi khi `hienTai` không chuyển được sang `moi` (đã biết hienTai không phải trạng thái cuối). */
    static String lyDoKhongChuyenDuoc(int hienTai, int moi) {
        if (moi == hienTai) {
            // thường do hai người cùng bấm, hoặc bấm đúp: người thứ hai đến sau
            return "Hóa đơn đã ở trạng thái \"" + ten(hienTai) + "\" rồi. Vui lòng tải lại trang.";
        }
        if (moi == HoaDon.DA_HUY) {
            return "Đơn đã giao cho đơn vị vận chuyển, không thể hủy.";
        }
        if (moi == HoaDon.GIAO_THAT_BAI) {
            return "Chỉ báo giao hàng thất bại được với đơn giao hàng đang ở trạng thái \"" + ten(HoaDon.DANG_GIAO) + "\".";
        }
        return "Không thể chuyển từ \"" + ten(hienTai) + "\" sang \"" + ten(moi) + "\". Vui lòng tải lại trang.";
    }

    /** Câu báo lỗi khi đổi trạng thái của hóa đơn đã ở trạng thái cuối. */
    static String lyDoDaKetThuc(int hienTai) {
        return switch (hienTai) {
            case HoaDon.DA_HUY -> "Hóa đơn đã bị hủy, không thể đổi trạng thái nữa.";
            case HoaDon.HOAN_THANH -> "Hóa đơn đã hoàn thành, không thể đổi trạng thái nữa.";
            default -> "Hóa đơn đã ở trạng thái \"" + ten(hienTai) + "\", không thể đổi trạng thái nữa.";
        };
    }

    /** Lý do mặc định ghi vào lịch sử khi người thao tác không nhập ghi chú. */
    static String moTaMacDinh(int hienTai, int moi) {
        if (hienTai == HoaDon.GIAO_THAT_BAI && moi == HoaDon.DANG_GIAO) {
            return "Giao lại sau khi giao hàng thất bại";
        }
        return "Chuyển sang \"" + ten(moi) + "\"";
    }
}
