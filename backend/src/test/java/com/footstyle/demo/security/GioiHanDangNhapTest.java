package com.footstyle.demo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class GioiHanDangNhapTest {

    private Instant bayGio = Instant.parse("2026-10-16T02:00:00Z");
    private final GioiHanDangNhap g = new GioiHanDangNhap();

    private void chinhGio(Duration them) {
        bayGio = bayGio.plus(them);
        g.datDongHo(Clock.fixed(bayGio, ZoneId.of("UTC")));
    }

    @Test
    void sai5LanThiKhoa5Phut_roiMoLai() {
        chinhGio(Duration.ZERO);
        assertEquals(4, g.ghiSai("long.nh@footstyle.vn"));
        assertEquals(3, g.ghiSai("LONG.NH@footstyle.vn"), "không phân biệt hoa thường");
        assertEquals(2, g.ghiSai(" long.nh@footstyle.vn "));
        assertEquals(1, g.ghiSai("long.nh@footstyle.vn"));
        assertEquals(0, g.conKhoaGiay("long.nh@footstyle.vn"), "4 lần sai chưa khóa");
        assertEquals(0, g.ghiSai("long.nh@footstyle.vn"));
        assertEquals(300, g.conKhoaGiay("long.nh@footstyle.vn"));
        chinhGio(Duration.ofMinutes(4));
        assertTrue(g.conKhoaGiay("long.nh@footstyle.vn") > 0);
        chinhGio(Duration.ofMinutes(1));
        assertEquals(0, g.conKhoaGiay("long.nh@footstyle.vn"), "hết 5 phút thì mở");
        assertEquals(4, g.ghiSai("long.nh@footstyle.vn"), "đếm lại từ đầu");
    }

    @Test
    void dangNhapDung_xoaBoDem_taiKhoanKhacKhongAnhHuong() {
        chinhGio(Duration.ZERO);
        g.ghiSai("a@x.vn");
        g.ghiSai("a@x.vn");
        g.ghiDung("a@x.vn");
        assertEquals(4, g.ghiSai("a@x.vn"));
        for (int i = 0; i < 5; i++) g.ghiSai("b@x.vn");
        assertTrue(g.conKhoaGiay("b@x.vn") > 0);
        assertEquals(0, g.conKhoaGiay("a@x.vn"));
    }
}
