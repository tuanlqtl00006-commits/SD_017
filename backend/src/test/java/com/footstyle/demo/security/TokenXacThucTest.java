package com.footstyle.demo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class TokenXacThucTest {

    private static final String BI_MAT = "chuoi-bi-mat-de-test-123456";
    private static final Instant GOC = Instant.parse("2026-10-16T01:00:00Z");

    private static TokenXacThuc tao(Instant luc) {
        return new TokenXacThuc(BI_MAT, Duration.ofHours(12), Clock.fixed(luc, ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    @Test
    void taoRoiDocLai_dungThongTin() {
        TokenXacThuc t = tao(GOC);
        String token = t.tao("NV", 3, "$2a$10$abc");
        TokenXacThuc.KetQua kq = t.doc(token);
        assertTrue(kq.hopLe());
        assertEquals("NV", kq.thongTin().loai());
        assertEquals(3, kq.thongTin().id());
        assertEquals(TokenXacThuc.vanTay("$2a$10$abc"), kq.thongTin().vanTay());
        assertEquals(GOC.plus(Duration.ofHours(12)).getEpochSecond(), kq.thongTin().hetHan());
    }

    @Test
    void sua1KyTuTrongDuLieu_saiChuKy() {
        TokenXacThuc t = tao(GOC);
        String token = t.tao("NV", 3, "$2a$10$abc");
        String phan1 = new String(Base64.getUrlDecoder().decode(token.substring(0, token.indexOf('.'))), StandardCharsets.UTF_8);
        // đổi id 3 -> 1 (giả làm quản lý) nhưng giữ chữ ký cũ
        String gia = Base64.getUrlEncoder().withoutPadding().encodeToString(phan1.replace("|3|", "|1|").getBytes(StandardCharsets.UTF_8))
                + token.substring(token.indexOf('.'));
        assertEquals(TokenXacThuc.Loi.SAI_CHU_KY, t.doc(gia).loi());
    }

    @Test
    void khacChuoiBiMat_saiChuKy() {
        String token = tao(GOC).tao("NV", 3, "x");
        TokenXacThuc khac = new TokenXacThuc("mot-chuoi-bi-mat-khac-hoan-toan", Duration.ofHours(12), Clock.fixed(GOC, ZoneId.of("UTC")));
        assertEquals(TokenXacThuc.Loi.SAI_CHU_KY, khac.doc(token).loi());
    }

    @Test
    void quaHan_bao_HET_HAN() {
        String token = tao(GOC).tao("NV", 3, "x");
        assertTrue(tao(GOC.plus(Duration.ofHours(11))).doc(token).hopLe());
        assertEquals(TokenXacThuc.Loi.HET_HAN, tao(GOC.plus(Duration.ofHours(12))).doc(token).loi());
        assertEquals(TokenXacThuc.Loi.HET_HAN, tao(GOC.plus(Duration.ofDays(3))).doc(token).loi());
    }

    @Test
    void chuoiRacHoacRong() {
        TokenXacThuc t = tao(GOC);
        assertEquals(TokenXacThuc.Loi.THIEU, t.doc(null).loi());
        assertEquals(TokenXacThuc.Loi.THIEU, t.doc("  ").loi());
        for (String rac : new String[] {"abc", "a.b.c", ".abc", "abc.", "!!!.???", "eyJhbGciOiJIUzI1NiJ9.e30"}) {
            TokenXacThuc.KetQua kq = t.doc(rac);
            assertFalse(kq.hopLe(), rac);
        }
    }

    @Test
    void vanTayDoiKhiMatKhauDoi() {
        assertNotEquals(TokenXacThuc.vanTay("$2a$10$cu"), TokenXacThuc.vanTay("$2a$10$moi"));
        assertEquals(TokenXacThuc.vanTay("$2a$10$cu"), TokenXacThuc.vanTay("$2a$10$cu"));
        assertEquals(12, TokenXacThuc.vanTay("abc").length());
        assertEquals("-", TokenXacThuc.vanTay(null));
    }

    @Test
    void chuoiBiMatQuaNgan_baoLoiNgayKhiKhoiDong() {
        assertThrows(IllegalArgumentException.class, () -> new TokenXacThuc("ngan", Duration.ofHours(1), Clock.systemUTC()));
        assertThrows(IllegalArgumentException.class, () -> new TokenXacThuc(null, Duration.ofHours(1), Clock.systemUTC()));
    }
}
