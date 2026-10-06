<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';
import Sidebar from './Sidebar.vue';

const route = useRoute();
const idHoaDon = route.params.id;

const hoaDonInfo = ref(null);
const danhSachSanPham = ref([]);

const lichSuThaoTac = ref([]);
const formatCurrency = (value) => {
  if (!value) return '0 ₫';
  return new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
};

const currentStatus = ref(1);

const timelineSteps = ref([
  { id: 1, trangThaiCode: 1, title: 'Chờ Xác Nhận', icon: 'bi-hourglass-split', time: null, date: null, user: null },
  { id: 2, trangThaiCode: 2, title: 'Đã Xác Nhận', icon: 'bi-clipboard-check', time: null, date: null, user: null },
  { id: 3, trangThaiCode: 3, title: 'Chờ Lấy Hàng', icon: 'bi-box-seam', time: null, date: null, user: null },
  { id: 4, trangThaiCode: 4, title: 'Đang Giao Hàng', icon: 'bi-truck', time: null, date: null, user: null },
  { id: 5, trangThaiCode: 5, title: 'Đã Giao Hàng', icon: 'bi-check-circle', time: null, date: null, user: null },
  { id: 6, trangThaiCode: 6, title: 'Hoàn Thành', icon: 'bi-flag', time: null, date: null, user: null },
]);

const trangThaiBadgeText = computed(() => {
  if (currentStatus.value === 0) return 'Đã Hủy';
  const step = timelineSteps.value.find(s => s.id === currentStatus.value);
  return step ? step.title : 'Không xác định';
});

const trangThaiBadgeClass = computed(() => {
  switch (currentStatus.value) {
    case 0: return 'bg-danger-subtle text-danger';
    case 1: return 'bg-warning-subtle text-warning';
    case 6: return 'bg-success-subtle text-success';
    default: return 'bg-primary-subtle text-primary';
  }
});

const selectedStatusMoi = ref(1);

const moPopupCapNhat = () => {
  selectedStatusMoi.value = currentStatus.value; 
};

const thayDoiTrangThai = async (trangThaiMoi, cauHoiXacNhan) => {
  if (confirm(cauHoiXacNhan)) {
    try {
      await axios.put(`http://localhost:8080/api/hoa-don/cap-nhat-trang-thai/${idHoaDon}?trangThaiMoi=${trangThaiMoi}`);
      
      alert("Cập nhật trạng thái thành công!");
      
      await fetchChiTietHoaDon(); 
      
    } catch (error) {
      console.error(error);
      alert("Lỗi: Không thể cập nhật trạng thái.");
    }
  }
};

const luuTrangThaiTuPopup = () => {
  thayDoiTrangThai(
    selectedStatusMoi.value, 
    `Bạn có chắc chắn muốn cập nhật đơn hàng sang trạng thái này?`
  );
};

const calculateProgressWidth = () => {
  const totalSteps = timelineSteps.value.length;
  if (currentStatus.value <= 1) return 0;
  if (currentStatus.value >= totalSteps) return 100;
  return ((currentStatus.value - 1) / (totalSteps - 1)) * 100;
};

const dongBoTimeline = (trangThaiHienTai, mangLichSu) => {
  const stepHienTai = timelineSteps.value.find(s => s.trangThaiCode === trangThaiHienTai);
  if (stepHienTai) {
    currentStatus.value = stepHienTai.id; 
  }

  if (mangLichSu && mangLichSu.length > 0) {
    mangLichSu.forEach(lichSu => {
      const step = timelineSteps.value.find(s => s.trangThaiCode === lichSu.trangThai);
      if (step) {
        const dateObj = new Date(lichSu.ngayTao);
        step.time = dateObj.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
        step.date = dateObj.toLocaleDateString('vi-VN');
        step.user = lichSu.nguoiTao || 'ADMIN';
      }
    });
  }
};


const fetchChiTietHoaDon = async () => {
  try {
    const response = await axios.get(`http://localhost:8080/api/hoa-don/${idHoaDon}`);
    
    console.log("Dữ liệu API thật:", response.data);

    const dataAPI = response.data;
    hoaDonInfo.value = dataAPI.hoaDon || dataAPI.HoaDon || dataAPI;
    
    currentStatus.value = hoaDonInfo.value?.trangThai || 1;
    
    danhSachSanPham.value = dataAPI.chiTietList || dataAPI.danhSachSanPham || [];

    if (response.data.danhSachLichSu && response.data.danhSachLichSu.length > 0) {
      lichSuThaoTac.value = response.data.danhSachLichSu.map(item => {
        const dateObj = new Date(item.ngayTao);
        return {
          nguoiTao: item.nguoiTao || 'ADMIN',
          thoiGian: dateObj.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) + ' ' + dateObj.toLocaleDateString('vi-VN'),
          hanhDong: item.ghiChu || 'Cập nhật trạng thái'
        };
      });
      
      dongBoTimeline(hoaDonInfo.value.trangThai, response.data.danhSachLichSu);
    } else {
      lichSuThaoTac.value = [];
      dongBoTimeline(hoaDonInfo.value.trangThai, []);
    }

  } catch (error) {
    console.error("Lỗi:", error);
  }
};

onMounted(() => {
  fetchChiTietHoaDon();
});
</script>



<template>
  <div class="d-flex" style="min-height: 100vh; background-color: #f4f7f6;">

    <Sidebar />


    
    <div class="flex-grow-1 d-flex flex-column overflow-hidden">

      
      <header class="bg-white border-bottom px-4 py-3 d-flex justify-content-between align-items-center z-1">
        <div class="d-flex align-items-center gap-3">
          <button class="btn btn-light border-0 d-lg-none"><i class="bi bi-list fs-4"></i></button>
          <div class="fw-medium text-muted fs-6">
            Hóa đơn <span class="mx-1">/</span> <span class="text-dark fw-bold">Chi tiết hóa đơn</span>
          </div>
        </div>

        <div class="d-flex align-items-center gap-3">
          <button class="btn btn-outline-primary btn-sm rounded-pill px-3 fw-medium"><i class="bi bi-clock me-1"></i> Ca làm việc</button>
          <button class="btn btn-light rounded-circle p-2 text-muted shadow-sm"><i class="bi bi-moon"></i></button>
          <div class="position-relative">
            <button class="btn btn-light rounded-circle p-2 text-muted shadow-sm"><i class="bi bi-bell"></i></button>
            <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.6rem;">4</span>
          </div>
          <div class="d-flex align-items-center gap-2 ms-2 cursor-pointer">
            <img src="https://ui-avatars.com/api/?name=Admin&background=0D6EFD&color=fff" class="rounded-circle shadow-sm" width="35" height="35" alt="Avatar">
            <span class="fw-medium text-dark d-none d-md-block">Admin</span>
          </div>
        </div>
      </header>

      
      <main class="p-4 overflow-auto">

        
        <div class="row g-4 mb-4">
          
          <div class="col-xl-8">
            
            <div class="card border-0 shadow-sm rounded-4 mb-4">
              <div class="card-header bg-white border-bottom-0 pt-4 pb-0 d-flex justify-content-between align-items-center">
                <h6 class="fw-bold m-0 text-dark">
                  <i class="bi bi-file-earmark-text text-secondary me-2"></i>Trạng Thái Đơn Hàng
                </h6>
                
                <span class="badge rounded-pill px-3 py-2 fw-medium" :class="trangThaiBadgeClass">
                  {{ trangThaiBadgeText }}
                </span>
              </div>

              <div class="card-body p-4 pt-5">
                <div class="position-relative w-100 mb-5">
                  
                  <div class="position-absolute top-0 start-0 w-100 mt-4" style="height: 2px; background-color: #e9ecef; z-index: 1;"></div>
                  
                  
                  <div class="position-absolute top-0 start-0 mt-4" 
                       style="height: 2px; background-color: #0d6efd; z-index: 1; transition: 0.5s ease;"
                       :style="{ width: calculateProgressWidth() + '%' }">
                  </div>

                  
                  <div class="d-flex justify-content-between position-relative w-100" style="z-index: 2;">
                    
                    <div v-for="(step, index) in timelineSteps" :key="index" class="text-center" style="width: 14%;">
                      
                      
                      
                      <div class="rounded-circle d-flex align-items-center justify-content-center mx-auto mb-3 transition-all"
                           :class="step.id <= currentStatus ? 'bg-primary text-white shadow' : 'bg-white border border-2 border-primary text-primary'"
                           style="width: 55px; height: 55px;">
                        <i class="bi fs-4" :class="step.icon"></i>
                      </div>

                      
                      <div :class="step.id <= currentStatus ? 'text-primary' : 'text-muted'">
                        <div class="fw-bold mb-1" style="font-size: 0.9rem;">{{ step.title }}</div>
                        
                        
                        <div v-if="step.id <= currentStatus" class="d-flex flex-column" style="font-size: 0.75rem;">
                          <span class="text-muted opacity-75">{{ step.time }}</span>
                          <span class="text-muted opacity-75">{{ step.date }}</span>
                          <span class="text-muted mt-1">{{ step.user }}</span>
                        </div>
                        
                      </div>
                    </div>

                  </div>
                </div> 

                
                <div class="text-end mt-4">
                  
                  <button class="btn btn-primary btn-sm px-3 py-2 rounded-3 shadow-sm" type="button" data-bs-toggle="modal" data-bs-target="#modalLichSuThaoTac">
                    <i class="bi bi-clock-history me-2"></i> Lịch Sử Thao Tác
                  </button>
                </div>
              </div>
            </div>
          </div>

          
          <div class="col-xl-4">
            <div class="card border-0 shadow-sm rounded-4 h-100">
              <div class="card-body p-4">
                <h6 class="fw-bold mb-3"><i class="bi bi-receipt-cutoff text-primary me-2"></i>Tổng Kết Thanh Toán</h6>
                
                <div v-if="hoaDonInfo">
                  <div class="d-flex justify-content-between mb-2">
                    <span class="text-muted">Tổng Tiền Hàng</span>
                    <span class="fw-medium">{{ formatCurrency(hoaDonInfo.tongTien) }}</span>
                  </div>
                  
                  <div class="d-flex justify-content-between mb-2">
                    <span class="text-muted">Phiếu Giảm Giá</span>
                    
                    <span class="fw-medium text-success">- {{ formatCurrency(hoaDonInfo.tienGiamGia || 0) }}</span>
                  </div>
                  
                  <div class="d-flex justify-content-between mb-3">
                    <span class="text-muted">Phí Vận Chuyển</span>
                    <span class="fw-medium">+ {{ formatCurrency(hoaDonInfo.phiVanChuyen || 0) }}</span>
                  </div>
                  
                  <hr>
                  
                  <div class="d-flex justify-content-between align-items-center">
                    <h6 class="fw-bold mb-0">Khách Cần Trả</h6>
                    <h5 class="fw-bold text-danger mb-0">
                      
                      {{ formatCurrency(hoaDonInfo.tongTien - (hoaDonInfo.tienGiamGia || 0) + (hoaDonInfo.phiVanChuyen || 0)) }}
                    </h5>
                  </div>
                </div>
                
                <div v-else class="text-center text-muted mt-3 placeholder-glow">
                  <span class="placeholder col-12"></span><span class="placeholder col-8"></span>
                </div>
              </div>
            </div>
          </div>
        </div>

        
        <div class="row g-4 mb-4">
          
          <div class="col-xl-4">
            <div class="card border-0 shadow-sm rounded-4 h-100">
              
              <div class="card-body p-4">
                <h6 class="fw-bold mb-3"><i class="bi bi-person-fill text-primary me-2"></i>Thông Tin Khách Hàng</h6>
                
                
                <div v-if="hoaDonInfo">
                  <div class="d-flex justify-content-between mb-2">
                    <span class="text-muted small">Tên Khách Hàng</span>
                    
                    <span class="fw-medium">{{ hoaDonInfo.tenKhachHang || 'Khách lẻ' }}</span>
                  </div>
                  
                  <div class="d-flex justify-content-between mb-2">
                    <span class="text-muted small">Số Điện Thoại</span>
                    <span class="fw-medium">{{ hoaDonInfo.sdtNguoiNhan || 'Không có' }}</span>
                  </div>
                  
                  <div class="d-flex justify-content-between">
                    <span class="text-muted small">Email</span>
                    
                    <span class="fw-medium">{{ hoaDonInfo.email || 'Không có' }}</span>
                  </div>
                </div>
                
                
                <div v-else class="text-center text-muted mt-3 placeholder-glow">
                  <span class="placeholder col-12"></span>
                  <span class="placeholder col-8"></span>
                </div>
              </div>
            </div>
          </div>

          
          <div class="col-xl-4">
            <div class="card border-0 shadow-sm rounded-4 h-100">
              
              <div class="card-body p-4">
                <h6 class="fw-bold mb-3"><i class="bi bi-geo-alt-fill text-primary me-2"></i>Thông Tin Giao Hàng</h6>
                
                <div v-if="hoaDonInfo">
                  <div class="d-flex justify-content-between mb-2">
                    <span class="text-muted small">Địa Chỉ</span>
                    
                    <span class="fw-medium text-end" style="max-width: 65%;">
                      {{ hoaDonInfo.loaiDon === 'Online' ? (hoaDonInfo.diaChi || 'Chưa cập nhật') : 'Nhận tại quầy' }}
                    </span>
                  </div>
                  
                  <div class="d-flex justify-content-between mb-2">
                    <span class="text-muted small">Loại Đơn</span>
                    <span class="fw-medium">
                      
                      <span class="badge" :class="hoaDonInfo.loaiDon === 'Online' ? 'bg-info' : 'bg-secondary'">
                        {{ hoaDonInfo.loaiDon === 'Online' ? 'Giao hàng' : 'Tại quầy' }}
                      </span>
                    </span>
                  </div>
                  
                  <div class="d-flex justify-content-between">
                    <span class="text-muted small">Ghi Chú</span>
                    <span class="fw-medium text-end">{{ hoaDonInfo.ghiChu || 'Không có' }}</span>
                  </div>
                </div>
                
                <div v-else class="text-center text-muted mt-3 placeholder-glow">
                  <span class="placeholder col-12"></span><span class="placeholder col-8"></span>
                </div>
              </div>
            </div>
          </div>

          
          <div class="col-xl-4">
            
            <div class="card border-0 shadow-sm rounded-4 mt-4">
              <div class="card-body">
                <h6 class="fw-bold mb-3"><i class="bi bi-credit-card-2-front text-primary me-2"></i>Lịch Sử Thanh Toán</h6>
                
                <div v-if="hoaDonInfo">
                  <div class="d-flex justify-content-between align-items-start">
                    <div>
                      <div class="fw-bold fs-6">{{ hoaDonInfo.phuongThucThanhToan || 'Tiền mặt' }}</div>
                      <div class="small text-muted mb-1">Thanh toán đơn hàng</div>
                      
                      <div class="small text-muted">{{ hoaDonInfo.ngayTao || '---' }}</div>
                    </div>
                    <div class="text-end">
                      <span class="badge mb-2" :class="hoaDonInfo.trangThaiThanhToan === 1 ? 'bg-success-subtle text-success' : 'bg-warning-subtle text-warning'">
                        {{ hoaDonInfo.trangThaiThanhToan === 1 ? 'Đã thanh toán' : 'Chưa thanh toán' }}
                      </span>
                      <div class="fw-bold fs-5 text-dark">
                        {{ formatCurrency(hoaDonInfo.tongTien - (hoaDonInfo.tienGiamGia || 0) + (hoaDonInfo.phiVanChuyen || 0)) }}
                      </div>
                    </div>
                  </div>
                </div>

                
                <div v-else class="text-center text-muted mt-2 placeholder-glow">
                  <span class="placeholder col-12"></span><span class="placeholder col-8"></span>
                </div>
              </div>
            </div>

            
            <div class="card border-0 shadow-sm rounded-4 mt-3">
              <div class="card-body p-3">
                <div class="d-grid gap-2">
                  
                  
                  <button class="btn btn-outline-primary border-primary-subtle py-2 rounded-3" type="button">
                    <i class="bi bi-printer me-2"></i>In Hóa Đơn
                  </button>
                  
                  
                  <button v-if="currentStatus !== 0 && currentStatus !== 6" 
                          class="btn btn-primary shadow-sm rounded-3 py-2" 
                          data-bs-toggle="modal" 
                          data-bs-target="#modalCapNhatDonHang" 
                          @click="moPopupCapNhat">
                    Cập nhật đơn hàng
                  </button>

                </div>
              </div>
            </div>
          </div>
        </div>

        
        
        <div class="card border-0 shadow-sm rounded-4 mt-4 mb-4">
          <div class="card-body p-0">
            <div class="p-3 border-bottom d-flex align-items-center bg-light rounded-top-4">
              <h6 class="fw-bold mb-0"><i class="bi bi-box-seam text-primary me-2"></i>Danh Sách Sản Phẩm</h6>
              <span class="badge bg-primary ms-2 rounded-pill">{{ danhSachSanPham.length }}</span>
            </div>
            
            <div class="table-responsive">
              <table class="table table-hover align-middle mb-0">
                <thead class="table-light text-muted small text-uppercase">
                  <tr>
                    <th class="py-3 border-0 rounded-start text-center">STT</th>
                    <th class="py-3 border-0">Mã Vợt</th>
                    <th class="py-3 border-0 text-center">Ảnh</th>
                    <th class="py-3 border-0">Tên Sản Phẩm</th>
                    <th class="py-3 border-0 text-center">Màu Sắc</th>
                    <th class="py-3 border-0 text-center">Trọng Lượng</th>
                    <th class="py-3 border-0 text-center">Chu Vi Cán</th>
                    <th class="py-3 border-0 text-center">Số Lượng</th>
                    <th class="py-3 border-0 text-end">Đơn Giá</th>
                    <th class="py-3 border-0 text-end rounded-end">Thành Tiền</th>
                  </tr>
                </thead>
                
                <tbody>
                  <tr v-for="(sp, index) in danhSachSanPham" :key="index">
                    <td class="py-3 border-0 text-center fw-medium">{{ index + 1 }}</td>
                    
                    <td class="py-3 border-0 fw-medium text-primary">{{ sp.maSPCT || '---' }}</td>
                    
                    <td class="py-3 border-0 text-center">
                      <div class="bg-light rounded-2 d-flex align-items-center justify-content-center border mx-auto" style="width: 45px; height: 45px;">
                        <i v-if="!sp.hinhAnh" class="bi bi-image text-muted fs-5"></i>
                        <img v-else :src="sp.hinhAnh" alt="Ảnh SP" class="img-fluid rounded-2" style="object-fit: cover; width: 100%; height: 100%;">
                      </div>
                    </td>
                    
                    <td class="py-3 border-0 fw-bold text-dark">{{ sp.tenSanPham || '---' }}</td>
                    
                    
                    <td class="py-3 border-0 text-center">{{ sp.mauSac || '---' }}</td>
                    <td class="py-3 border-0 text-center">{{ sp.trongLuong || '---' }}</td>
                    <td class="py-3 border-0 text-center">{{ sp.chuVi || '---' }}</td>
                    
                    <td class="py-3 border-0 text-center fw-bold">{{ sp.soLuong || 0 }}</td>
                    
                    <td class="py-3 border-0 text-end text-muted">{{ formatCurrency(sp.donGia || 0) }}</td>
                    
                    <td class="py-3 border-0 text-end fw-bold text-danger">
                      {{ formatCurrency((sp.donGia || 0) * (sp.soLuong || 0)) }}
                    </td>
                  </tr>

                  <tr v-if="danhSachSanPham.length === 0">
                    <td colspan="10" class="text-center py-5 text-muted">
                      Chưa có sản phẩm nào được tải!
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

      </main>
    </div>
  
    <div class="modal fade" id="modalCapNhatDonHang" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-4 border-0 shadow">
          
          <div class="modal-header border-bottom-0 pb-0">
            <h6 class="modal-title fw-bold">Cập nhật thông tin đơn hàng</h6>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>

          <div class="modal-body pt-2">
            
            <ul class="nav nav-tabs mb-4 border-bottom" style="font-size: 0.9rem;">
              <li class="nav-item">
                <a class="nav-link fw-medium text-dark border-0 border-bottom border-2 border-dark" href="#" style="background: transparent;">Thông tin đơn hàng</a>
              </li>
              <li class="nav-item">
                <a class="nav-link text-primary border-0" href="#">Thông tin khách hàng</a>
              </li>
            </ul>

            
            <div class="row mb-3">
              <div class="col-6">
                <label class="form-label text-muted" style="font-size: 0.85rem;">Mã đơn hàng</label>
                
                <input type="text" class="form-control bg-light border-0" :value="hoaDonInfo?.maHoaDon || 'HD1786874725111'" readonly>
              </div>
              <div class="col-6">
                <label class="form-label text-muted" style="font-size: 0.85rem;">Ngày tạo</label>
                <input type="text" class="form-control bg-light border-0" :value="hoaDonInfo?.ngayTao || '17:05:25 16/08/2026'" readonly>
              </div>
            </div>

            <div class="mb-2">
              <label class="form-label text-muted" style="font-size: 0.85rem;">Trạng thái</label>
              <select class="form-select shadow-sm" v-model="selectedStatusMoi">
                
                <option v-for="step in timelineSteps" :key="step.id" :value="step.trangThaiCode">
                  {{ step.title }}
                </option>
                <option :value="0">Hủy đơn hàng</option>
              </select>
            </div>
          </div>

          <div class="modal-footer border-top-0 pt-0">
            <button type="button" class="btn btn-secondary px-4 rounded-3" data-bs-dismiss="modal" style="background-color: #6c757d;">Hủy</button>
            <button type="button" class="btn btn-danger px-4 rounded-3" data-bs-dismiss="modal" @click="luuTrangThaiTuPopup" style="background-color: #b02a37;">Lưu</button>
          </div>

        </div>
      </div>
    </div>

  
    <div class="modal fade" id="modalLichSuThaoTac" tabindex="-1" aria-hidden="true">
      
      <div class="modal-dialog modal-dialog-centered modal-lg modal-dialog-scrollable">
        <div class="modal-content rounded-4 border-0 shadow">
          
          
          <div class="modal-header border-bottom-0 bg-primary-subtle bg-opacity-10 rounded-top-4 pb-3">
            <div class="d-flex align-items-center">
              <div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center me-3" style="width: 40px; height: 40px;">
                <i class="bi bi-clock-history fs-5"></i>
              </div>
              <div>
                <h5 class="modal-title fw-bold text-dark mb-0 d-flex align-items-center">
                  Lịch sử thao tác
                  
                  <span class="badge bg-primary-subtle text-primary rounded-pill ms-2 fs-6 fw-normal">
                    {{ lichSuThaoTac?.length || 0 }} hoạt động
                  </span>
                </h5>
                <p class="text-muted mb-0" style="font-size: 0.85rem;">Theo dõi người thực hiện và nội dung thay đổi trên hóa đơn</p>
              </div>
            </div>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>

          
          <div class="modal-body bg-light pt-4 pb-4">
            
            
            <div v-if="lichSuThaoTac && lichSuThaoTac.length > 0" class="position-relative px-3">
              
              <div class="position-absolute h-100 border-start border-2 border-secondary border-opacity-25" style="left: 24px; top: 0; z-index: 1;"></div>

              
              
              <div v-for="(item, index) in (lichSuThaoTac || []).slice().reverse()" :key="index" class="position-relative mb-4" style="z-index: 2; padding-left: 30px;">
                
                
                <div class="position-absolute bg-primary rounded-circle border border-3 border-white shadow-sm" style="width: 14px; height: 14px; left: -6px; top: 12px;"></div>
                
                
                <div class="card border-0 shadow-sm rounded-3">
                  <div class="card-body p-3">
                    
                    <div class="d-flex justify-content-between align-items-start mb-2">
                      
                      <div class="d-flex align-items-center">
                        <div class="bg-primary-subtle text-primary rounded-circle d-flex align-items-center justify-content-center me-2" style="width: 35px; height: 35px;">
                          <i class="bi bi-person-check-fill"></i>
                        </div>
                        <div>
                          <div class="fw-bold" style="font-size: 0.9rem;">
                            {{ item.nguoiTao }}
                          </div>
                          
                          <span class="badge rounded-pill border border-primary text-primary" style="font-size: 0.65rem; background-color: transparent;">NHÂN VIÊN</span>
                        </div>
                      </div>
                      
                      
                      <div class="text-muted d-flex align-items-center" style="font-size: 0.8rem;">
                        <i class="bi bi-clock me-1"></i> {{ item.thoiGian }}
                      </div>
                    </div>

                    
                    <div class="fw-bold mt-2 text-dark" style="font-size: 0.95rem;">
                      {{ item.hanhDong }}
                    </div>
                    
                  </div>
                </div>

              </div>
              

            </div>

            
            <div v-else class="text-center py-5">
              <div class="mb-3">
                
                <i class="bi bi-inbox text-secondary opacity-25" style="font-size: 4rem;"></i>
              </div>
              <h6 class="fw-bold text-muted mb-1">Chưa có lịch sử thao tác nào</h6>
              <p class="text-muted" style="font-size: 0.85rem;">Các hoạt động thay đổi trạng thái sẽ xuất hiện ở đây.</p>
            </div>
            
          </div>
          
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

.sidebar-menu .sidebar-link {
color: #495057;
padding: 0.7rem 1rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  text-decoration: none;
  transition: all 0.2s ease-in-out;
}

.sidebar-menu .sidebar-link .icon {
  font-size: 1.15rem;
  margin-right: 0.8rem;
  color: #6c757d; 
  width: 24px;
  text-align: center;
}


.sidebar-menu .sidebar-link:hover:not(.active) {
  background-color: #f8f9fa;
  color: #0d6efd; 
}

.sidebar-menu .sidebar-link:hover:not(.active) .icon,
.sidebar-menu .sidebar-link:hover:not(.active) .chevron {
  color: #0d6efd;
}


.sidebar-menu .sidebar-link.active {
  background-color: #0d6efd !important; 
  color: white !important; 
  font-weight: 600;
  box-shadow: 0 0.125rem 0.25rem rgba(13, 110, 253, 0.2); 
}

.sidebar-menu .sidebar-link.active .icon {
  color: white !important;
}


.submenu .sidebar-link {
  padding: 0.6rem 1rem 0.6rem 2.8rem; 
  font-size: 0.9rem;
  color: #6c757d;
}

.submenu .sidebar-link .icon {
  font-size: 1rem;
  margin-right: 0.6rem;
  color: #adb5bd;
}


.sidebar-menu .sidebar-link .chevron {
  transition: transform 0.3s ease;
  font-size: 0.75rem;
  color: #6c757d;
}


.sidebar-menu .sidebar-link:not(.collapsed) .chevron {
transform: rotate(180deg);
  color: #0d6efd; 
}


.timeline-container {
  position: relative;
  padding: 0 20px;
}

.timeline-line {
  position: absolute;
  top: 25px; 
  left: 10%;
  right: 10%;
  height: 3px;
  background-color: #e9ecef;
  z-index: 0;
}
.timeline-step {
  position: relative;
  z-index: 1;
  text-align: center;
  width: 120px; 
}
.timeline-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  font-size: 1.4rem;
  transition: all 0.3s ease;
}

.timeline-step.active .timeline-icon {
  box-shadow: 0 0 0 5px rgba(13, 110, 253, 0.15);
}
</style>