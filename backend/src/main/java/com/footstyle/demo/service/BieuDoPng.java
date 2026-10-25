package com.footstyle.demo.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Vẽ biểu đồ cột nhóm (2 chuỗi: kỳ này / kỳ so sánh) ra ảnh PNG để nhúng vào email báo cáo thống kê
 * (thư điện tử không chạy được JavaScript nên không dùng biểu đồ của trang web được). Chạy được ở chế độ headless.
 */
final class BieuDoPng {

    static final Color MAU_A = new Color(0x09, 0x77, 0xEC);
    static final Color MAU_B = new Color(0xB8, 0xC4, 0xD6);

    private BieuDoPng() {
    }

    /** Một nhóm cột: nhãn trục hoành + giá trị chuỗi A (kỳ này) + chuỗi B (kỳ so sánh, null = không có). */
    record Cot(String nhan, long a, Long b) {
    }

    static byte[] cotNhom(String tieuDe, String tenA, String tenB, List<Cot> cot) {
        int w = 760;
        int h = 340;
        int trai = 70;
        int phai = 20;
        int tren = 56;
        int duoi = 44;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setColor(new Color(0x18, 0x24, 0x2F));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            g.drawString(tieuDe, trai, 24);

            // chú thích
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            int x = trai;
            x = chuThich(g, x, 42, MAU_A, tenA);
            if (tenB != null) chuThich(g, x + 18, 42, MAU_B, tenB);

            long max = 1;
            for (Cot c : cot) max = Math.max(max, Math.max(c.a(), c.b() == null ? 0 : c.b()));
            long tran = lamTronTran(max);
            int caoVe = h - tren - duoi;
            g.setStroke(new BasicStroke(1f));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i <= 4; i++) {
                int y = h - duoi - caoVe * i / 4;
                g.setColor(new Color(0xE3, 0xEA, 0xF3));
                g.drawLine(trai, y, w - phai, y);
                g.setColor(new Color(0x7A, 0x86, 0x96));
                String nhan = nhanTienGon(tran * i / 4);
                g.drawString(nhan, trai - 8 - fm.stringWidth(nhan), y + 4);
            }
            int n = Math.max(1, cot.size());
            double rongNhom = (double) (w - trai - phai) / n;
            boolean coB = tenB != null;
            double rongCot = rongNhom * (coB ? 0.34 : 0.6);
            int buocNhan = Math.max(1, (int) Math.ceil(n / 14.0));
            for (int i = 0; i < cot.size(); i++) {
                Cot c = cot.get(i);
                double x0 = trai + i * rongNhom + rongNhom * (coB ? 0.14 : 0.2);
                veCot(g, x0, c.a(), tran, caoVe, h - duoi, rongCot, MAU_A);
                if (coB && c.b() != null) veCot(g, x0 + rongCot + 2, c.b(), tran, caoVe, h - duoi, rongCot, MAU_B);
                if (i % buocNhan == 0) {
                    g.setColor(new Color(0x7A, 0x86, 0x96));
                    int tw = fm.stringWidth(c.nhan());
                    g.drawString(c.nhan(), (int) (trai + i * rongNhom + rongNhom / 2 - tw / 2.0), h - duoi + 18);
                }
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Không vẽ được biểu đồ: " + e.getMessage(), e);
        }
    }

    private static int chuThich(Graphics2D g, int x, int y, Color mau, String ten) {
        g.setColor(mau);
        g.fillRoundRect(x, y - 10, 12, 12, 3, 3);
        g.setColor(new Color(0x47, 0x55, 0x69));
        g.drawString(ten, x + 18, y);
        return x + 18 + g.getFontMetrics().stringWidth(ten);
    }

    private static void veCot(Graphics2D g, double x, long v, long tran, int caoVe, int day, double rong, Color mau) {
        int cao = (int) Math.round((double) v / tran * caoVe);
        g.setColor(mau);
        g.fillRoundRect((int) x, day - cao, Math.max(2, (int) rong), cao, 4, 4);
    }

    /** 1, 2, 5 x 10^n (giống trục của trang Thống kê). */
    static long lamTronTran(long max) {
        if (max <= 0) return 1;
        long mu = (long) Math.pow(10, Math.floor(Math.log10(max)));
        double f = (double) max / mu;
        long b = f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10;
        return b * mu;
    }

    static String nhanTienGon(long v) {
        if (v == 0) return "0";
        if (v >= 1_000_000_000L) return gon(v / 1e9) + " tỷ";
        if (v >= 1_000_000L) return gon(v / 1e6) + "tr";
        if (v >= 1_000L) return gon(v / 1e3) + "k";
        return String.valueOf(v);
    }

    private static String gon(double x) {
        double r = Math.round(x * 10) / 10.0;
        return (r == Math.floor(r) ? String.valueOf((long) r) : String.valueOf(r)).replace('.', ',');
    }
}
