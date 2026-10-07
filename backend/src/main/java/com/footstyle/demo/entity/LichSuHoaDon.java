package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Date;

/** Bảng lich_su_hoa_don: mỗi lần đổi trạng thái / thao tác trên hóa đơn ghi một dòng. */
@Entity
@Table(name = "lich_su_hoa_don")
@Getter
@Setter
public class LichSuHoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_hoa_don")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private HoaDon hoaDon;

    @Column(name = "nguoi_thao_tac")
    private String nguoiThaoTac;

    /** Tên hành động dạng chữ, vd "Chờ xác nhận", "Đã xác nhận", "Hủy đơn hàng" (cột NVARCHAR). */
    @Column(name = "hanh_dong")
    private String hanhDong;

    // Added to support old properties if accessed
    private Integer trangThai;

    @Column(name = "thoi_gian")
    private LocalDateTime thoiGian;

    // Optional field to avoid compile error if Date was used
    private Date ngayTao;

    @Column(name = "mo_ta")
    private String moTa;

    // To support older alias
    public void setNguoiTao(String nguoiTao) {
        this.nguoiThaoTac = nguoiTao;
    }
    public String getNguoiTao() {
        return this.nguoiThaoTac;
    }
    public void setGhiChu(String ghiChu) {
        this.moTa = ghiChu;
    }
    public String getGhiChu() {
        return this.moTa;
    }
}
