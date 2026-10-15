package com.footstyle.demo.service;

import java.security.SecureRandom;

/** Mật khẩu tạm ngẫu nhiên cho tài khoản nhân viên tạo hàng loạt (gửi qua mail, nhân viên nên đổi sau khi đăng nhập). */
public final class MatKhauTam {

    // Bỏ các ký tự dễ nhìn nhầm (0/O, 1/l/I) để nhân viên gõ lại từ mail không bị sai
    private static final String HOA = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String THUONG = "abcdefghijkmnpqrstuvwxyz";
    private static final String SO = "23456789";
    private static final String TAT_CA = HOA + THUONG + SO;
    private static final SecureRandom NGAU_NHIEN = new SecureRandom();

    public static final int DO_DAI = 10;

    private MatKhauTam() {
    }

    /** 10 ký tự, luôn có chữ hoa, chữ thường và chữ số (đạt luật mật khẩu của nhân viên: 8-50 ký tự, có chữ và số). */
    public static String tao() {
        char[] kq = new char[DO_DAI];
        kq[0] = HOA.charAt(NGAU_NHIEN.nextInt(HOA.length()));
        kq[1] = THUONG.charAt(NGAU_NHIEN.nextInt(THUONG.length()));
        kq[2] = SO.charAt(NGAU_NHIEN.nextInt(SO.length()));
        for (int i = 3; i < DO_DAI; i++) kq[i] = TAT_CA.charAt(NGAU_NHIEN.nextInt(TAT_CA.length()));
        for (int i = DO_DAI - 1; i > 0; i--) { // xáo trộn để 3 ký tự bắt buộc không luôn đứng đầu
            int j = NGAU_NHIEN.nextInt(i + 1);
            char t = kq[i];
            kq[i] = kq[j];
            kq[j] = t;
        }
        return new String(kq);
    }
}
