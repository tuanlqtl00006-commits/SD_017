<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

import Sidebar from './Sidebar.vue';
import Header from './Header.vue';

const router = useRouter();

const danhSachHoaDon = ref([]);
const currentPage = ref(0); 
const totalPages = ref(1);  

const currentTab = ref(null);

// Hàm xem chi tiết giờ chỉ làm nhiệm vụ chuyển trang
const xemChiTiet = (idHoaDon) => {
  // Chuyển hướng sang trang chi tiết và truyền ID lên thanh URL
  router.push(`/hoa-don/chi-tiet/${idHoaDon}`); 
};

const formatCurrency = (value) => {
  if (!value) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
};

const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('vi-VN');
};


const fetchHoaDon = async (page = 0) => {
  try {
    const response = await axios.get('http://localhost:8080/api/hoa-don', {
      params: {
        page: page,
        size: 5,
        maHoaDon: filter.value.maHoaDon || null,
        tuNgay: filter.value.tuNgay || null,
        denNgay: filter.value.denNgay || null,
        loaiDon: filter.value.loaiDon !== '' ? filter.value.loaiDon : null,
        
        // Dòng quyết định lọc theo Tabs
        trangThai: activeTab.value !== null ? activeTab.value : null
      }
    });
    danhSachHoaDon.value = response.data.content;
    totalPages.value = response.data.totalPages;
    currentPage.value = response.data.number;
  } catch (error) {
    console.error("Lỗi khi load dữ liệu:", error);
  }
};


const changePage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    fetchHoaDon(page);
  }
};


const filter = ref({
  maHoaDon: '',
  tuNgay: '',
  denNgay: '',
  loaiDon: ''
});

// 1. THÊM BIẾN NÀY ĐỂ QUẢN LÝ TAB (null là Tất cả)
const activeTab = ref(null);

// 2. THÊM HÀM CHUYỂN TAB
const changeTab = (status) => {
  activeTab.value = status;
  fetchHoaDon(0); // Chuyển tab thì tự động reset về trang 1
};


const tuNgayInput = ref(null);
const denNgayInput = ref(null);


const openDatePicker = (element) => {
  if (element && typeof element.showPicker === 'function') {
    element.showPicker();
  }
};


const handleSearch = () => {
  fetchHoaDon(0); 
};


const handleReset = () => {
  filter.value = { maHoaDon: '', tuNgay: '', denNgay: '', loaiDon: '' };
  fetchHoaDon(0); 
};


const handleExport = () => {
  if (danhSachHoaDon.value.length === 0) {
    alert("Không có dữ liệu để xuất file!");
    return;
  }
  
  
  let csvContent = "\uFEFFSTT,Mã Hóa Đơn,Tên Khách Hàng,Tên Nhân Viên,Tổng Tiền,Ngày Tạo,Loại Đơn\n";

  
  danhSachHoaDon.value.forEach((hd, index) => {
    let row = [
      index + 1,
      hd.maHoaDon,
      hd.tenKhachHang,
      hd.tenNhanVien,
      hd.tongTien,
      formatDate(hd.ngayTao).replace(',', ''), 
      hd.loaiDon
    ].join(",");
    csvContent += row + "\n";
  });

  
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.setAttribute("download", "DanhSachHoaDon.csv");
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

onMounted(() => {
  fetchHoaDon(0); 
});
</script>
<template>
  <div class="d-flex" style="min-height: 100vh; background-color: #f4f7f6;">

    
    <Sidebar />

    <div class="flex-grow-1 d-flex flex-column">

      
      <Header />

      
      <main class="p-4">

        <div class="bg-white border rounded-3 shadow-sm py-3 px-4 mb-4">
          <h5 class="fw-bold text-primary m-0">Quản lý hóa đơn</h5>
        </div>

        <div class="card border-0 shadow-sm rounded-4 mb-4">
          <div class="card-body p-4">
            <h6 class="fw-bold mb-3 d-flex align-items-center"><i class="bi bi-funnel text-muted me-2 fs-5"></i> Bộ Lọc</h6>
            <div class="row g-3">
              <div class="col-md-3">
                <label class="form-label small text-dark fw-medium">Mã hóa đơn</label>
                
                <input type="text" class="form-control" placeholder="Nhập mã hóa đơn" v-model="filter.maHoaDon">
              </div>
              
              <div class="col-md-3">
                <label class="form-label small text-dark fw-medium">Ngày Bắt Đầu</label>
                <div class="input-group shadow-sm">
                  
                  <input type="date" class="form-control border-end-0 text-dark" 
                         v-model="filter.tuNgay" 
                         ref="tuNgayInput"
                         @click="openDatePicker(tuNgayInput)"
                         style="cursor: pointer; box-shadow: none;">
                  
                  <span class="input-group-text bg-white text-primary border-start-0" 
                        @click="openDatePicker(tuNgayInput)"
                        style="cursor: pointer;">
                    <i class="bi bi-calendar-event"></i>
                  </span>
                </div>
              </div>

              
              <div class="col-md-3">
                <label class="form-label small text-dark fw-medium">Ngày Kết Thúc</label>
                <div class="input-group shadow-sm">
                  <input type="date" class="form-control border-end-0 text-dark" 
                         v-model="filter.denNgay" 
                         ref="denNgayInput"
                         @click="openDatePicker(denNgayInput)"
                         style="cursor: pointer; box-shadow: none;">
                  <span class="input-group-text bg-white text-primary border-start-0" 
                        @click="openDatePicker(denNgayInput)"
                        style="cursor: pointer;">
                    <i class="bi bi-calendar-event"></i>
                  </span>
                </div>
              </div>
              <div class="col-md-3">
                <label class="form-label small text-dark fw-medium">Loại Đơn</label>
                <select class="form-select text-muted" v-model="filter.loaiDon">
                  <option value="">Tất cả Loại Đơn</option>
                  <option value="1">Tại quầy</option>
                  <option value="2">Online</option>
                </select>
              </div>
            </div>

            <div class="d-flex justify-content-end gap-3 mt-4">
              
              <button class="btn btn-primary px-4 shadow-sm" @click="handleSearch">Tìm kiếm</button>
              <button class="btn btn-danger px-4 shadow-sm" @click="handleReset">Làm mới</button>
              <button class="btn btn-success px-4 shadow-sm" @click="handleExport">
                <i class="bi bi-file-earmark-arrow-down me-1"></i> Xuất File
              </button>
            </div>
          </div>
        </div>

        <div class="card rounded-3 shadow-sm bg-white" style="border: 2px solid #5b8deb;">
          <div class="card-body p-0">

            <div class="d-flex align-items-center px-4 pt-4 pb-3">
              <div class="bg-secondary bg-opacity-25 text-primary rounded-3 d-flex align-items-center justify-content-center me-3" style="width: 45px; height: 45px;">
                <i class="bi bi-file-earmark-text fs-4 text-secondary"></i>
              </div>
              <h4 class="fw-bold m-0 text-dark" style="font-size: 1.25rem;">Danh Sách Hóa Đơn</h4>
            </div>

<!-- DẢI TABS TRẠNG THÁI -->
            <ul class="nav custom-tabs d-flex justify-content-between flex-nowrap overflow-auto px-4 w-100" style="border-bottom: 1px solid #e9ecef;">
              <li class="nav-item">
                <a class="nav-link text-center px-2" :class="{ active: activeTab === null }" href="#" @click.prevent="changeTab(null)">Tất Cả</a>
              </li>
              <li class="nav-item">
                <a class="nav-link text-center px-2" :class="{ active: activeTab === 1 }" href="#" @click.prevent="changeTab(1)">Chờ Xác Nhận</a>
              </li>
              <li class="nav-item">
                <a class="nav-link text-center px-2" :class="{ active: activeTab === 2 }" href="#" @click.prevent="changeTab(2)">Đã Xác Nhận</a>
              </li>
              <li class="nav-item">
                <a class="nav-link text-center px-2" :class="{ active: activeTab === 3 }" href="#" @click.prevent="changeTab(3)">Chờ Vận Chuyển</a>
              </li>
              <li class="nav-item">
                <a class="nav-link text-center px-2" :class="{ active: activeTab === 4 }" href="#" @click.prevent="changeTab(4)">Vận Chuyển</a>
              </li>
              <li class="nav-item">
                <a class="nav-link text-center px-2" :class="{ active: activeTab === 5 }" href="#" @click.prevent="changeTab(5)">Đã Hoàn Thành</a>
              </li>
              <li class="nav-item">
                <a class="nav-link text-center px-2" :class="{ active: activeTab === 0 }" href="#" @click.prevent="changeTab(0)">Hủy</a>
              </li>
            </ul>

            <div class="table-responsive">
              <table class="table align-middle m-0 text-start">
                <thead style="background-color: #f4f6f9;">
                <tr>
                  <th class="py-3 text-center fw-bold text-dark border-0" style="width: 80px;">STT</th>
                  <th class="py-3 fw-bold text-dark border-0">Mã Hóa Đơn</th>
                  <th class="py-3 fw-bold text-dark border-0">Tên Khách Hàng</th>
                  <th class="py-3 fw-bold text-dark border-0">Tên Nhân Viên</th>
                  <th class="py-3 fw-bold text-dark border-0">Tổng Tiền</th>
                  <th class="py-3 fw-bold text-dark border-0">Ngày Tạo</th>
                  <th class="py-3 fw-bold text-dark border-0">Loại Đơn</th>
                  <th class="py-3 text-center fw-bold text-dark border-0" style="width: 120px;">Hành Động</th>
                </tr>
                </thead>
                <tbody class="text-dark">
                
                <tr v-if="danhSachHoaDon.length === 0">
                    
                    <td colspan="8" class="text-center py-4 text-danger fw-medium">
                       Không tìm thấy hóa đơn nào phù hợp với điều kiện lọc!
                    </td>
                  </tr>

                
                <tr v-for="(hd, index) in danhSachHoaDon" :key="hd.id">
                  <td class="py-4 text-center" style="border-bottom: 1px solid #f0f0f0;">{{ index + 1 }}</td>

                  <td class="py-4 fw-medium" style="border-bottom: 1px solid #f0f0f0;">{{ hd.maHoaDon }}</td>

                  
                  <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ hd.tenKhachHang }}</td>

                  
                  <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ hd.tenNhanVien }}</td>

                  
                  <td class="py-4 fw-medium text-danger" style="border-bottom: 1px solid #f0f0f0;">{{ formatCurrency(hd.tongTien) }}</td>

                  
                  <td class="py-4" style="border-bottom: 1px solid #f0f0f0;" v-html="formatDate(hd.ngayTao).replace(' ', '<br>')"></td>

                  <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ hd.loaiDon }}</td>

                  <!-- Cột Hành Động -->
                  <td class="align-middle">
                    <button class="btn btn-sm btn-outline-primary border-0" 
                            title="Xem chi tiết" 
                            @click="xemChiTiet(hd.id)">
                      <i class="bi bi-eye fs-5"></i>
                    </button>
                  </td>
                </tr>
                </tbody>
              </table>
            </div>

            
            <div class="d-flex justify-content-end px-4 py-4 mt-2">
              <nav v-if="totalPages > 0">
                <ul class="pagination custom-pagination m-0 gap-2 align-items-center">
                  
                  
                  <li class="page-item" :class="{ disabled: currentPage === 0 }">
                    <a class="page-link rounded-2 bg-white text-dark" href="#" @click.prevent="changePage(currentPage - 1)">
                      <i class="bi bi-chevron-left"></i>
                    </a>
                  </li>

                  
                  <li class="page-item">
                    <div class="page-link rounded-2 bg-white text-dark fw-bold d-flex align-items-center justify-content-center shadow-sm" 
                         style="border: 2px solid #a3c5ff; min-width: 40px; height: 40px; cursor: default; user-select: none;">
                      {{ currentPage + 1 }}
                    </div>
                  </li>

                  
                  <li class="page-item" :class="{ disabled: currentPage === totalPages - 1 }">
                    <a class="page-link rounded-2 bg-white text-dark" href="#" @click.prevent="changePage(currentPage + 1)">
                      <i class="bi bi-chevron-right"></i>
                    </a>
                  </li>
                  
                </ul>
              </nav>
            </div>

          </div>
        </div>

      </main>
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


.form-control::placeholder {
  color: #ced4da;
}


.custom-tabs .nav-link {
  color: #858796;
  border: none;
  border-bottom: 2px solid transparent;
  padding: 1rem 0.5rem;
  font-weight: 500;
  font-size: 0.95rem;
}
.custom-tabs .nav-link.active {
  color: #0d6efd !important;
  border-bottom: 2px solid #0d6efd;
  font-weight: 600;
}
.custom-tabs .nav-link:hover {
  color: #0d6efd;
}


.custom-pagination .page-link {
  color: #495057;
  border: 1px solid #e9ecef;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
}
.custom-pagination .page-item.disabled .page-link {
  color: #adb5bd;
  background-color: #fff;
}


input[type="date"]::-webkit-calendar-picker-indicator {
    display: none;
    -webkit-appearance: none;
}
</style>