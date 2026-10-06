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
@Table(name = "chu_vi")
@AttributeOverride(name = "ma", column = @Column(name = "ma_chu_vi"))
@AttributeOverride(name = "ten", column = @Column(name = "ten_chu_vi"))
public class ChuVi extends ThuocTinh {
}
