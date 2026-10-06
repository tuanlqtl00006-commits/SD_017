package com.footstyle.demo.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "xuat_xu")
@AttributeOverride(name = "ma", column = @Column(name = "ma_xuat_xu"))
@AttributeOverride(name = "ten", column = @Column(name = "ten_xuat_xu"))
public class XuatXu extends ThuocTinh {
}
