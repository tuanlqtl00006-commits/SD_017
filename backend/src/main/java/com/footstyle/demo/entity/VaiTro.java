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

    
    public static final String TEN_QUAN_LY = "Quản lý";
}
