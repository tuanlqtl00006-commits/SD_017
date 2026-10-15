package com.footstyle.demo;

import java.lang.reflect.Proxy;
import java.util.function.BiFunction;

/**
 * Kho (repository) giả cho test: chỉ trả lời các hàm được khai báo, gọi hàm khác thì báo lỗi ngay
 * để test không vô tình chạy qua một nhánh chưa được giả lập. Không cần CSDL, Spring hay Mockito.
 */
public final class KhoGia {

    /** Trả về giá trị này nghĩa là "hàm chưa được giả lập". */
    public static final Object KHONG_CO = new Object();

    private KhoGia() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T tao(Class<T> kieu, BiFunction<String, Object[], Object> xuLy) {
        return (T) Proxy.newProxyInstance(kieu.getClassLoader(), new Class<?>[] {kieu}, (p, m, args) -> {
            switch (m.getName()) {
                case "toString": return kieu.getSimpleName() + " giả";
                case "hashCode": return System.identityHashCode(p);
                case "equals": return p == args[0];
                default: break;
            }
            Object kq = xuLy.apply(m.getName(), args == null ? new Object[0] : args);
            if (kq == KHONG_CO) throw new UnsupportedOperationException(kieu.getSimpleName() + "." + m.getName());
            return kq;
        });
    }

    /** Kho không được dùng tới trong test. */
    public static <T> T khongDung(Class<T> kieu) {
        return tao(kieu, (ten, a) -> KHONG_CO);
    }
}
