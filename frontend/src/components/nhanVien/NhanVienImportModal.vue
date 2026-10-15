<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import { nhanVienService } from '../../services/nhanVienService'
import { useToast } from '../../composables/useToast'
import { exportExcel } from '../../utils/exportExcel'
import { todayIso } from '../../utils/format'
import { HUONG_DAN_TUOI_NHAN_VIEN } from '../../utils/kiemTra'
import { COT_TAI_KHOAN, TOI_DA_DONG_NHAP, taiFileMau } from '../../utils/mauNhapNhanVien'

/*
 * NHẬP HÀNG LOẠT NHÂN VIÊN TỪ EXCEL - theo góp ý của cô:
 *  - Sai dù chỉ MỘT dòng thì CẢ FILE bị từ chối (chưa có nhân viên nào được thêm); báo rõ dòng nào, cột nào, sai gì.
 *  - Kiểm tra định dạng và trùng (trong file và với nhân viên đã có) bằng đúng bộ luật của form Thêm nhân viên (backend NhanVienImportService).
 *  - Nhập xong hệ thống tự gửi mail tài khoản (mật khẩu tạm) cho từng nhân viên; hộp thoại hiện tiến độ gửi mail.
 *
 * Các bước: chon (tải file mẫu, chọn file) -> loi (file bị từ chối: bảng lỗi)  hoặc  xem (file hợp lệ: xem trước danh sách) -> xong (kết quả + mail).
 */
const props = defineProps({
  // Cứ chừng này mili giây hỏi lại tiến độ gửi mail một lần (đổi được để test chạy nhanh)
  chuKyMailMs: { type: Number, default: 1500 },
})
const emit = defineEmits(['close', 'imported'])
const toast = useToast()

const DUNG_LUONG_TOI_DA = 5 * 1024 * 1024 // 5 MB: file 100 nhân viên chỉ vài chục KB
const MA_LOI_FILE = 'FILE_KHONG_HOP_LE' // khớp NhanVienImportService.MA_LOI_FILE
const SO_LAN_HOI_MAIL_TOI_DA = 60

const buoc = ref('chon') // chon | loi | xem | xong
const file = ref(null)
const khoaOChonFile = ref(0) // đổi số này để ô chọn file được làm sạch
const dangXuLy = ref(false)
const loiChung = ref('') // lỗi không gắn với dòng nào: sai đuôi file, thiếu cột, quá 100 dòng, mất kết nối...
const loiFile = ref(null) // { thongBao, loi: [{ dong, truong, cot, thongBao }], tongDong, soDongLoi }
const xemTruoc = ref(null) // kết quả lần kiểm tra
const ketQua = ref(null) // kết quả lần nhập thật
const mail = ref(null) // { tong, daGui, loi, giaLap, xong, lyDo }
const mailQuaHan = ref(false)
const hienMatKhau = ref(false)

function veBuocChon() {
  buoc.value = 'chon'
  file.value = null
  khoaOChonFile.value++
  loiChung.value = ''
  loiFile.value = null
  xemTruoc.value = null
}

function chonFile(e) {
  const f = e.target.files?.[0] ?? null
  loiChung.value = ''
  loiFile.value = null
  file.value = null
  if (!f) return
  if (!/\.xlsx$/i.test(f.name)) {
    loiChung.value = 'Chỉ nhận file Excel định dạng .xlsx. Hãy mở file trong Excel, chọn Lưu thành → Excel Workbook (.xlsx) rồi chọn lại.'
    e.target.value = ''
    return
  }
  if (f.size > DUNG_LUONG_TOI_DA) {
    loiChung.value = 'File quá lớn (tối đa 5 MB). Hãy tách thành nhiều file, mỗi file tối đa ' + TOI_DA_DONG_NHAP + ' nhân viên.'
    e.target.value = ''
    return
  }
  file.value = f
}

// Lỗi từ backend: 422 FILE_KHONG_HOP_LE kèm danh sách dòng sai -> bảng lỗi; lỗi khác (thiếu cột, quá nhiều dòng, mất mạng) -> một câu báo
function xuLyLoi(err, buocHienTai) {
  if (err?.code === MA_LOI_FILE && Array.isArray(err.chiTiet?.loi)) {
    loiFile.value = { thongBao: err.message, loi: err.chiTiet.loi, tongDong: err.chiTiet.tongDong, soDongLoi: err.chiTiet.soDongLoi }
    loiChung.value = ''
    buoc.value = 'loi'
    return
  }
  loiChung.value = err?.message || 'Không xử lý được file. Vui lòng thử lại.'
  buoc.value = buocHienTai
}

async function kiemTraFile() {
  if (!file.value || dangXuLy.value) return
  dangXuLy.value = true
  loiChung.value = ''
  loiFile.value = null
  try {
    xemTruoc.value = await nhanVienService.importExcel(file.value, { chiKiemTra: true })
    buoc.value = 'xem'
  } catch (err) {
    xuLyLoi(err, 'chon')
  } finally {
    dangXuLy.value = false
  }
}

async function nhapVaoHeThong() {
  if (!file.value || dangXuLy.value) return
  dangXuLy.value = true
  loiChung.value = ''
  try {
    ketQua.value = await nhanVienService.importExcel(file.value, { chiKiemTra: false })
    buoc.value = 'xong'
    toast.success(`Đã thêm ${ketQua.value.soNhanVien} nhân viên từ file Excel.`)
    emit('imported', ketQua.value)
    if (ketQua.value.maLo) theoDoiMail(ketQua.value.maLo)
  } catch (err) {
    // Trong lúc xem trước có người khác vừa thêm trùng email... -> máy chủ kiểm tra lại và từ chối: hiện bảng lỗi
    xuLyLoi(err, 'xem')
  } finally {
    dangXuLy.value = false
  }
}

/* ----- Tiến độ gửi mail tài khoản ----- */
let daDong = false
onBeforeUnmount(() => {
  daDong = true
})
const cho = (ms) => new Promise((r) => setTimeout(r, ms))

async function theoDoiMail(maLo) {
  mail.value = { tong: ketQua.value.soMail, daGui: 0, loi: 0, giaLap: 0, xong: false, lyDo: '' }
  for (let lan = 0; lan < SO_LAN_HOI_MAIL_TOI_DA && !daDong; lan++) {
    try {
      const kq = await nhanVienService.ketQuaMailImport(maLo)
      if (daDong) return
      mail.value = kq
      if (kq.xong) return
    } catch {
      /* mất kết nối thoáng qua: lần sau hỏi lại */
    }
    await cho(props.chuKyMailMs)
  }
  if (!daDong && !mail.value?.xong) mailQuaHan.value = true
}

const mailDaXong = computed(() => !!mail.value?.xong)
const mailGiaLap = computed(() => (mail.value?.giaLap ?? 0) > 0 || ketQua.value?.mailDaCauHinh === false)
const soMailCoKetQua = computed(() => (mail.value?.daGui ?? 0) + (mail.value?.loi ?? 0) + (mail.value?.giaLap ?? 0))

function taiDanhSachTaiKhoan() {
  if (!ketQua.value?.taiKhoan?.length) return
  exportExcel({
    filename: `tai_khoan_nhan_vien_moi_${todayIso()}.xlsx`,
    sheetName: 'Tài khoản',
    columns: COT_TAI_KHOAN,
    rows: ketQua.value.taiKhoan,
  })
}

function dong() {
  emit('close')
}
</script>

<template>
  <BaseModal title="Nhập nhân viên từ Excel" size="xl" static-backdrop @close="dong">
    <ol class="nv-imp-steps" aria-label="Các bước nhập nhân viên">
      <li :class="{ 'is-on': buoc === 'chon' || buoc === 'loi', 'is-done': buoc === 'xem' || buoc === 'xong' }">
        <span class="nv-imp-num">1</span> Chọn file
      </li>
      <li :class="{ 'is-on': buoc === 'xem', 'is-done': buoc === 'xong' }"><span class="nv-imp-num">2</span> Kiểm tra</li>
      <li :class="{ 'is-on': buoc === 'xong' }"><span class="nv-imp-num">3</span> Hoàn tất</li>
    </ol>

    <!-- ===== Bước 1: chọn file ===== -->
    <section v-if="buoc === 'chon'" data-testid="nv-import-chon">
      <div class="row g-3">
        <div class="col-lg-6">
          <h3 class="nv-imp-h">1. Tải file mẫu và điền thông tin</h3>
          <p class="ad-hint">Mỗi nhân viên một dòng. Cột có dấu (*) là bắt buộc. Hãy xóa hai dòng ví dụ trước khi nhập thật.</p>
          <button type="button" class="ad-btn" data-testid="nv-import-tai-mau" @click="taiFileMau">
            <i class="bi bi-download" aria-hidden="true"></i> Tải file mẫu (.xlsx)
          </button>

          <h3 class="nv-imp-h mt-4">2. Chọn file đã điền</h3>
          <label class="ad-label" for="nv-import-file">File Excel (.xlsx)</label>
          <input
            id="nv-import-file"
            :key="khoaOChonFile"
            type="file"
            class="form-control ad-control"
            accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            :disabled="dangXuLy"
            @change="chonFile"
          />
          <p v-if="file" class="ad-hint" data-testid="nv-import-ten-file"><i class="bi bi-file-earmark-excel" aria-hidden="true"></i> {{ file.name }}</p>
          <p v-if="loiChung" class="ad-error" role="alert" data-testid="nv-import-loi-chung">{{ loiChung }}</p>
        </div>

        <div class="col-lg-6">
          <div class="nv-imp-rules" data-testid="nv-import-quy-tac">
            <h3 class="nv-imp-h">Quy tắc nhập</h3>
            <ul>
              <li><strong>Sai dù chỉ một dòng thì cả file bị từ chối</strong> - chưa có nhân viên nào được thêm, hệ thống chỉ rõ dòng và cột sai.</li>
              <li>Kiểm tra giống form Thêm nhân viên: họ tên chỉ gồm chữ cái, số điện thoại đúng đầu số, email đúng định dạng, ngày sinh dd/mm/yyyy.</li>
              <li>{{ HUONG_DAN_TUOI_NHAN_VIEN }}</li>
              <li>Email, số điện thoại, số căn cước không được trùng nhau trong file và không trùng nhân viên đã có.</li>
              <li>Vai trò chỉ gồm <em>Quản lý</em> hoặc <em>Nhân viên</em> (bỏ trống = Nhân viên). Ngày vào làm bỏ trống = hôm nay.</li>
              <li>Tối đa {{ TOI_DA_DONG_NHAP }} nhân viên mỗi file.</li>
              <li>Nhập xong, hệ thống tự tạo mật khẩu tạm và <strong>gửi mail tài khoản</strong> cho từng nhân viên.</li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== File bị từ chối ===== -->
    <section v-else-if="buoc === 'loi' && loiFile" data-testid="nv-import-loi">
      <div class="alert alert-danger py-2 px-3 small" role="alert">
        <strong>Cả file bị từ chối - chưa có nhân viên nào được thêm.</strong>
        <div>
          Có <strong>{{ loiFile.loi.length }}</strong> lỗi ở <strong>{{ loiFile.soDongLoi }}/{{ loiFile.tongDong }}</strong> dòng. Hãy sửa các dòng dưới đây trong file Excel rồi chọn lại file.
        </div>
      </div>
      <div class="ad-table-wrap nv-imp-scroll">
        <div class="ad-table-scroll">
          <table class="ad-table" aria-label="Danh sách lỗi trong file">
            <thead>
              <tr>
                <th class="ad-col-stt">Dòng</th>
                <th>Cột</th>
                <th>Lỗi</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(l, i) in loiFile.loi" :key="i">
                <td class="ad-col-stt"><span class="ad-code">{{ l.dong }}</span></td>
                <td class="ad-nowrap">{{ l.cot }}</td>
                <td class="text-danger">{{ l.thongBao }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <p class="ad-hint mt-2">“Dòng” là số thứ tự dòng hiển thị bên trái trong Excel (dòng 1 là tiêu đề).</p>
    </section>

    <!-- ===== Bước 2: file hợp lệ, xem trước ===== -->
    <section v-else-if="buoc === 'xem' && xemTruoc" data-testid="nv-import-xem">
      <div class="alert alert-success py-2 px-3 small" role="status">
        <strong>File hợp lệ.</strong> Sẽ thêm <strong>{{ xemTruoc.soNhanVien }}</strong> nhân viên. Kiểm tra danh sách rồi bấm “Nhập vào hệ thống”.
      </div>
      <div v-if="xemTruoc.mailDaCauHinh === false" class="alert alert-warning py-2 px-3 small" role="status" data-testid="nv-import-canh-bao-mail">
        Hệ thống <strong>chưa cấu hình Gmail</strong> nên sẽ không gửi được mail thật. Sau khi nhập, hãy tải danh sách tài khoản (có mật khẩu tạm) để gửi cho nhân viên.
      </div>
      <div class="ad-table-wrap nv-imp-scroll">
        <div class="ad-table-scroll">
          <table class="ad-table" aria-label="Danh sách nhân viên sẽ được thêm">
            <thead>
              <tr>
                <th class="ad-col-stt">Dòng</th>
                <th>Mã dự kiến</th>
                <th>Họ tên</th>
                <th>Email đăng nhập</th>
                <th>Vai trò</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="x in xemTruoc.xemTruoc" :key="x.dong">
                <td class="ad-col-stt">{{ x.dong }}</td>
                <td><span class="ad-code ad-nowrap">{{ x.ma }}</span></td>
                <td class="ad-nowrap">{{ x.hoTen }}</td>
                <td>{{ x.email }}</td>
                <td class="ad-nowrap">{{ x.vaiTro }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <p v-if="loiChung" class="ad-error mt-2" role="alert" data-testid="nv-import-loi-chung">{{ loiChung }}</p>
    </section>

    <!-- ===== Bước 3: đã nhập xong ===== -->
    <section v-else-if="buoc === 'xong' && ketQua" data-testid="nv-import-xong">
      <div class="alert alert-success py-2 px-3 small" role="status">
        <strong>Đã thêm {{ ketQua.soNhanVien }} nhân viên.</strong> Mỗi người có một mật khẩu tạm do hệ thống tự tạo.
      </div>

      <div class="nv-imp-mail" data-testid="nv-import-mail" role="status" aria-live="polite">
        <template v-if="mailGiaLap">
          <i class="bi bi-envelope-exclamation text-warning" aria-hidden="true"></i>
          <span>Chưa cấu hình Gmail nên <strong>không gửi mail thật</strong>. Hãy tải danh sách tài khoản bên dưới và gửi mật khẩu tạm cho nhân viên.</span>
        </template>
        <template v-else-if="mailDaXong && !(mail.loi > 0)">
          <i class="bi bi-envelope-check text-success" aria-hidden="true"></i>
          <span>Đã gửi <strong>{{ mail.daGui }}/{{ mail.tong }}</strong> mail thông báo tài khoản.</span>
        </template>
        <template v-else-if="mailDaXong">
          <i class="bi bi-envelope-x text-danger" aria-hidden="true"></i>
          <span>
            Gửi mail lỗi <strong>{{ mail.loi }}/{{ mail.tong }}</strong><template v-if="mail.lyDo">: {{ mail.lyDo }}</template>. Hãy gửi mật khẩu tạm cho những người này theo cách khác (danh sách bên dưới).
          </span>
        </template>
        <template v-else-if="mailQuaHan">
          <i class="bi bi-envelope text-muted" aria-hidden="true"></i>
          <span>Mail vẫn đang được gửi ở nền, có thể mất thêm ít phút. Bạn có thể đóng cửa sổ này.</span>
        </template>
        <template v-else>
          <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
          <span>Đang gửi mail tài khoản… {{ soMailCoKetQua }}/{{ mail?.tong ?? ketQua.soMail }}</span>
        </template>
      </div>

      <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mt-3 mb-2">
        <h3 class="nv-imp-h m-0">Tài khoản vừa tạo</h3>
        <div class="d-flex gap-2">
          <button type="button" class="ad-btn ad-btn-sm" data-testid="nv-import-hien-mat-khau" @click="hienMatKhau = !hienMatKhau">
            <i class="bi" :class="hienMatKhau ? 'bi-eye-slash' : 'bi-eye'" aria-hidden="true"></i>
            {{ hienMatKhau ? 'Ẩn mật khẩu tạm' : 'Hiện mật khẩu tạm' }}
          </button>
          <button type="button" class="ad-btn ad-btn-sm ad-btn-primary" data-testid="nv-import-tai-tai-khoan" @click="taiDanhSachTaiKhoan">
            <i class="bi bi-file-earmark-excel" aria-hidden="true"></i> Tải danh sách tài khoản
          </button>
        </div>
      </div>
      <div class="ad-table-wrap nv-imp-scroll">
        <div class="ad-table-scroll">
          <table class="ad-table" aria-label="Tài khoản nhân viên vừa tạo">
            <thead>
              <tr>
                <th>Mã NV</th>
                <th>Họ tên</th>
                <th>Email đăng nhập</th>
                <th>Vai trò</th>
                <th>Mật khẩu tạm</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in ketQua.taiKhoan" :key="t.ma">
                <td><span class="ad-code ad-nowrap">{{ t.ma }}</span></td>
                <td class="ad-nowrap">{{ t.hoTen }}</td>
                <td>{{ t.email }}</td>
                <td class="ad-nowrap">{{ t.vaiTro }}</td>
                <td><code class="nv-imp-mk" data-testid="nv-import-mat-khau">{{ hienMatKhau ? t.matKhauTam : '••••••••••' }}</code></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <p class="ad-hint mt-2">
        <i class="bi bi-shield-lock" aria-hidden="true"></i>
        Mật khẩu tạm chỉ hiện <strong>một lần</strong> ở cửa sổ này (hệ thống chỉ lưu dạng đã mã hóa). Hãy tải danh sách và giữ an toàn.
      </p>
    </section>

    <template #footer>
      <template v-if="buoc === 'chon'">
        <button type="button" class="ad-btn" @click="dong">Hủy</button>
        <button type="button" class="ad-btn ad-btn-primary" data-testid="nv-import-kiem-tra" :disabled="!file || dangXuLy" @click="kiemTraFile">
          <span v-if="dangXuLy" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
          <i v-else class="bi bi-check2-circle" aria-hidden="true"></i>
          Kiểm tra file
        </button>
      </template>
      <template v-else-if="buoc === 'loi'">
        <button type="button" class="ad-btn" @click="dong">Đóng</button>
        <button type="button" class="ad-btn ad-btn-primary" data-testid="nv-import-chon-lai" @click="veBuocChon">
          <i class="bi bi-arrow-repeat" aria-hidden="true"></i> Chọn file khác
        </button>
      </template>
      <template v-else-if="buoc === 'xem'">
        <button type="button" class="ad-btn" :disabled="dangXuLy" data-testid="nv-import-chon-lai" @click="veBuocChon">Chọn file khác</button>
        <button type="button" class="ad-btn ad-btn-primary" data-testid="nv-import-nhap" :disabled="dangXuLy" @click="nhapVaoHeThong">
          <span v-if="dangXuLy" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
          Nhập {{ xemTruoc?.soNhanVien }} nhân viên vào hệ thống
        </button>
      </template>
      <template v-else>
        <button type="button" class="ad-btn ad-btn-primary" data-testid="nv-import-dong" @click="dong">Đóng</button>
      </template>
    </template>
  </BaseModal>
</template>

<style scoped>
.nv-imp-steps {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1.25rem;
  margin: 0 0 1.1rem;
  padding: 0;
  list-style: none;
  font-size: 0.85rem;
  color: var(--fs-muted);
}
.nv-imp-steps li {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}
.nv-imp-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  background: var(--fs-line-soft);
  font-weight: 600;
  font-size: 0.78rem;
}
.nv-imp-steps li.is-on {
  color: var(--fs-primary-dark);
  font-weight: 600;
}
.nv-imp-steps li.is-on .nv-imp-num {
  background: var(--fs-primary);
  color: #fff;
}
.nv-imp-steps li.is-done .nv-imp-num {
  background: var(--fs-primary-soft);
  color: var(--fs-primary-dark);
}
.nv-imp-h {
  margin: 0 0 0.4rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--fs-text);
}
.nv-imp-rules {
  height: 100%;
  padding: 1rem 1.1rem;
  border: 1px solid var(--fs-primary-line);
  border-radius: 14px;
  background: var(--fs-primary-soft);
  font-size: 0.85rem;
  color: var(--fs-text-2);
}
.nv-imp-rules ul {
  margin: 0;
  padding-left: 1.1rem;
  display: grid;
  gap: 0.35rem;
}
.nv-imp-scroll {
  max-height: 46vh;
  overflow: auto;
}
.nv-imp-scroll .ad-table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
}
.nv-imp-mail {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.7rem 0.9rem;
  border: 1px solid var(--fs-line);
  border-radius: 12px;
  background: var(--fs-surface);
  font-size: 0.88rem;
}
.nv-imp-mk {
  font-size: 0.85rem;
  color: var(--fs-text);
  letter-spacing: 0.3px;
}
</style>
