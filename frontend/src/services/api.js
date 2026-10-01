import axios from 'axios';

const api = axios.create({
    baseURL: '/api', // Dùng proxy trong vite.config.js để chuyển hướng sang http://localhost:8080/api
    headers: {
        'Content-Type': 'application/json',
    }
});

// Có thể cấu hình thêm Interceptors tại đây nếu cần (vd: gửi kèm Token đăng nhập)

export default api;