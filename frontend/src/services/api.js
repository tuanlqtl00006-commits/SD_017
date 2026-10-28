import axios from 'axios';
import { baoChuaVaoCa, baoHetPhien, baoVaiTro, layToken } from './phien';

const api = axios.create({
    baseURL: '/api', // Dùng proxy trong vite.config.js để chuyển hướng sang http://localhost:8081/api
    headers: {
        'Content-Type': 'application/json',
    }
});

// Gắn token đăng nhập vào mọi request (giai đoạn 3: backend chặn API khi chưa đăng nhập)
api.interceptors.request.use((config) => {
    const token = layToken();
    if (token && !config.headers?.Authorization) {
        config.headers = config.headers ?? {};
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Mã lỗi 401 của backend nghĩa là phải đăng nhập lại (thiếu / sai / hết hạn token, tài khoản bị ngừng, vừa đổi mật khẩu)
const MA_HET_PHIEN = ['CHUA_DANG_NHAP', 'HET_PHIEN', 'TOKEN_KHONG_HOP_LE'];

// Lỗi từ backend có dạng { message: "...", loiTheoTruong?: { tenO: "câu báo lỗi" }, code?, chiTiet? }:
//  - error.message      : câu tiếng Việt của backend (để các trang chỉ cần toast.error(e.message)); mất kết nối thì báo riêng cho dễ hiểu.
//  - error.status       : mã HTTP (400, 409, 422...) hoặc null khi không kết nối được máy chủ.
//  - error.loiTheoTruong: lỗi theo TỪNG Ô của form (gắn xuống dưới đúng ô nhập), null nếu backend không trả.
//  - error.code / error.chiTiet: mã lỗi nghiệp vụ và dữ liệu kèm theo (vd FILE_KHONG_HOP_LE + danh sách dòng sai khi nhập Excel).
api.interceptors.response.use(
    (response) => {
        baoVaiTro(response.headers?.['x-vai-tro']);
        return response;
    },
    (error) => {
        baoVaiTro(error.response?.headers?.['x-vai-tro']); // bị chặn 403 / 409 vì vừa bị đổi quyền cũng phải cập nhật
        const data = error.response?.data;
        if (data?.message) {
            error.message = data.message;
        } else if (!error.response) {
            error.message = 'Không kết nối được máy chủ. Hãy kiểm tra backend (cổng 8081) đã chạy chưa.';
        }
        error.status = error.response?.status ?? null;
        error.code = data?.code ?? null;
        error.loiTheoTruong = data?.loiTheoTruong && typeof data.loiTheoTruong === 'object' ? data.loiTheoTruong : null;
        error.chiTiet = data?.chiTiet ?? null;
        // Hết phiên: về trang đăng nhập (trừ chính lời gọi đăng nhập - sai mật khẩu cũng là 401 nhưng phải báo ngay trên form)
        if (error.status === 401 && MA_HET_PHIEN.includes(error.code) && !String(error.config?.url ?? '').includes('/auth/dang-nhap')) {
            baoHetPhien(error.message);
        }
        if (error.status === 409 && error.code === 'CHUA_NHAN_CA') baoChuaVaoCa();
        return Promise.reject(error);
    }
);

export default api;
