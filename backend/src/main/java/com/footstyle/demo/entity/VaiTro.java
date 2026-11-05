package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "vai_tro")
public class VaiTro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "ten_vai_tro")
    private String tenVaiTro;

    @Column(name = "trang_thai")
    private Integer trangThai;

    /** Tên vai trò dùng để nhận biết tài khoản quản lý (khớp dữ liệu mẫu trong database/SD_17_2_du_lieu.sql). */
    public static final String TEN_QUAN_LY = "Quản lý";
    public static final String TEN_NHAN_VIEN = "Nhân viên";
    public static final String TEN_THU_NGAN = "Thu ngân";
    public static final String TEN_BAN_HANG = "Nhân viên bán hàng";

    /** Hệ thống hỗ trợ Quản lý, Thu ngân (giữ tiền/mở ca), và Nhân viên bán hàng (tư vấn/lên đơn). */
    public static boolean laVaiTroHopLe(String ten) {
        if (ten == null) return false;
        String t = ten.trim();
        return TEN_QUAN_LY.equalsIgnoreCase(t)
                || TEN_NHAN_VIEN.equalsIgnoreCase(t)
                || TEN_THU_NGAN.equalsIgnoreCase(t)
                || TEN_BAN_HANG.equalsIgnoreCase(t)
                || "Bán hàng".equalsIgnoreCase(t);
    }
}
