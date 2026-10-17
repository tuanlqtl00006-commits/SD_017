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

    /** Hệ thống chỉ có HAI quyền: Quản lý (đủ chức năng) và Nhân viên (bán hàng, khách hàng, hóa đơn). */
    public static boolean laVaiTroHopLe(String ten) {
        return ten != null && (TEN_QUAN_LY.equalsIgnoreCase(ten.trim()) || TEN_NHAN_VIEN.equalsIgnoreCase(ten.trim()));
    }
}
