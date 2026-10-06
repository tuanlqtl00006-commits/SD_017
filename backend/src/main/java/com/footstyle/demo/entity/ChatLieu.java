package com.footstyle.demo.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Bảng chat_lieu: Chất liệu. Các cột id, trạng thái nằm ở lớp cha ThuocTinh. */
@Entity
@Getter
@Setter
@Table(name = "chat_lieu")
@AttributeOverride(name = "ma", column = @Column(name = "ma_chat_lieu"))
@AttributeOverride(name = "ten", column = @Column(name = "ten_chat_lieu"))
public class ChatLieu extends ThuocTinh {
}
