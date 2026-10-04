package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Lớp cha chung của 9 bảng thuộc tính sản phẩm (danh_muc, thuong_hieu, xuat_xu, chat_lieu, do_cung,
 * diem_can_bang, mau_sac, trong_luong, chu_vi). Cả 9 bảng cùng cấu trúc: id, mã, tên, trạng thái.
 * Tên cột "ma" và "ten" khác nhau theo từng bảng (ma_danh_muc, ma_thuong_hieu, ...) nên mỗi entity con
 * khai báo lại bằng @AttributeOverride.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class ThuocTinh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    private String ma;

    private String ten;

    @Column(name = "trang_thai")
    private Integer trangThai;

    public static final int HOAT_DONG = 1;        // trang_thai
    public static final int NGUNG_HOAT_DONG = 0;
}
