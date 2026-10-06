package com.footstyle.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;







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

    public static final int HOAT_DONG = 1;        
    public static final int NGUNG_HOAT_DONG = 0;
}
