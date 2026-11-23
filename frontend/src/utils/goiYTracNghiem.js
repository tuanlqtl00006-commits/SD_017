// Gợi ý sản phẩm dạng trắc nghiệm từng bước (góp ý 23/11 Task 3.1): khách trả lời trình độ, lối chơi, ngân sách,
// trọng lượng, thương hiệu -> lọc sản phẩm đang bán (dữ liệu thật từ /cua-hang/san-pham) và xếp theo mức phù hợp.
// Chỉ dùng thông số có trong CSDL (độ cứng, điểm cân bằng, giá sau giảm, trọng lượng biến thể còn hàng, thương hiệu, danh mục).

const boDau = (s) => String(s ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim()

export const CAU_HOI = [
  {
    khoa: 'trinhDo',
    hoi: 'Bạn chơi cầu lông ở mức nào?',
    moTa: 'Giúp chọn độ cứng thân vợt phù hợp với lực tay.',
    luaChon: [
      { gt: 'moi', nhan: 'Mới chơi', phu: 'Mới tập, lực tay còn yếu', icon: 'bi-emoji-smile' },
      { gt: 'phongTrao', nhan: 'Chơi phong trào', phu: 'Chơi đều đặn vài buổi mỗi tuần', icon: 'bi-people' },
      { gt: 'nangCao', nhan: 'Nâng cao / thi đấu', phu: 'Kỹ thuật tốt, lực tay khỏe', icon: 'bi-trophy' },
    ],
  },
  {
    khoa: 'loiChoi',
    hoi: 'Bạn thích lối chơi nào?',
    moTa: 'Điểm cân bằng của vợt quyết định cảm giác đánh.',
    luaChon: [
      { gt: 'tanCong', nhan: 'Tấn công', phu: 'Thích đập cầu mạnh', icon: 'bi-lightning-charge' },
      { gt: 'phongThu', nhan: 'Phòng thủ, phản tạt', phu: 'Đỡ cầu, đánh nhanh trên lưới', icon: 'bi-shield' },
      { gt: 'toanDien', nhan: 'Toàn diện', phu: 'Công thủ cân bằng', icon: 'bi-yin-yang' },
      { gt: 'chuaRo', nhan: 'Chưa rõ', phu: 'Để cửa hàng gợi ý', icon: 'bi-question-circle' },
    ],
  },
  {
    khoa: 'nganSach',
    hoi: 'Ngân sách của bạn khoảng bao nhiêu?',
    moTa: 'Tính theo giá đang bán (đã trừ giảm giá theo đợt).',
    luaChon: [
      { gt: 'duoi1', nhan: 'Dưới 1 triệu', tu: 0, den: 1_000_000, icon: 'bi-wallet' },
      { gt: '1den2', nhan: '1 - 2 triệu', tu: 1_000_000, den: 2_000_000, icon: 'bi-wallet2' },
      { gt: '2den4', nhan: '2 - 4 triệu', tu: 2_000_000, den: 4_000_000, icon: 'bi-cash-stack' },
      { gt: 'tren4', nhan: 'Trên 4 triệu', tu: 4_000_000, den: Infinity, icon: 'bi-gem' },
      { gt: 'tuyY', nhan: 'Không giới hạn', tu: 0, den: Infinity, icon: 'bi-infinity' },
    ],
  },
  {
    khoa: 'trongLuong',
    hoi: 'Bạn muốn vợt nặng bao nhiêu?',
    moTa: 'Vợt càng nhẹ càng dễ xoay trở; nặng hơn thì đầm tay, đập mạnh hơn.',
    luaChon: [
      { gt: '5U', nhan: 'Nhẹ - 5U (75-79g)', phu: 'Dễ đánh, hợp người mới, nữ', icon: 'bi-feather' },
      { gt: '4U', nhan: 'Vừa - 4U (80-84g)', phu: 'Phổ biến nhất', icon: 'bi-circle-half' },
      { gt: '3U', nhan: 'Nặng - 3U (85-89g)', phu: 'Đầm tay, đập mạnh', icon: 'bi-circle-fill' },
      { gt: 'khongRo', nhan: 'Không rõ', phu: 'Gợi ý theo trình độ', icon: 'bi-question-circle' },
    ],
  },
  {
    khoa: 'thuongHieu',
    hoi: 'Bạn thích thương hiệu nào?',
    moTa: 'Chọn một hoặc nhiều hãng, hoặc bỏ qua nếu không quan trọng.',
    nhieu: true, // chọn nhiều, được bỏ trống
    luaChon: [], // lấy từ dữ liệu sản phẩm đang bán
  },
]

const DO_CUNG_THEO_TRINH_DO = {
  moi: ['mem', 'trung binh'],
  phongTrao: ['trung binh', 'cung'],
  nangCao: ['cung', 'rat cung'],
}
const CAN_BANG_THEO_LOI_CHOI = { tanCong: 'nang dau', phongThu: 'nhe dau', toanDien: 'can bang' }
const TRONG_LUONG_THEO_TRINH_DO = { moi: ['5U', '4U'], phongTrao: ['4U'], nangCao: ['4U', '3U'] }

/** Thương hiệu đang có sản phẩm còn hàng (lựa chọn của câu cuối). */
export function dsThuongHieu(sanPhams) {
  return [...new Set((sanPhams || []).filter((p) => Number(p.tongSoLuongTon) > 0).map((p) => p.tenThuongHieu).filter(Boolean))].sort()
}

const maTrongLuong = (ten) => (String(ten || '').match(/^\s*(\d)\s*U/i) || [])[1]?.concat('U') || ''

/** Giá thấp nhất (sau giảm) của các biến thể còn hàng; không có dữ liệu biến thể thì lấy giaBanMin. */
function giaThapNhat(p) {
  const con = (p.bienThe || []).filter((b) => Number(b.soLuongTon) > 0)
  if (con.length) return Math.min(...con.map((b) => Number(b.giaBan) || 0))
  return Number(p.giaBanMin) || 0
}

/**
 * Chấm điểm và xếp sản phẩm theo câu trả lời. Chỉ lấy sản phẩm còn hàng; ngân sách là điều kiện bắt buộc
 * (giá thấp nhất còn hàng nằm trong khoảng). Mỗi tiêu chí khớp cộng điểm và ghi một "lý do" để hiện cho khách.
 * Trả về { ds: [{ sp, diem, lyDo, gia }], ngoaiNganSach: boolean } - ngoaiNganSach = true khi không có mẫu nào trong ngân sách
 * nên đang gợi ý các mẫu gần ngân sách nhất.
 */
export function goiYSanPham(sanPhams, tl, toiDa = 6) {
  const ns = CAU_HOI[2].luaChon.find((x) => x.gt === tl.nganSach) || { tu: 0, den: Infinity }
  const conHang = (sanPhams || []).filter((p) => Number(p.tongSoLuongTon) > 0)
  const doCung = DO_CUNG_THEO_TRINH_DO[tl.trinhDo] || []
  const canBang = CAN_BANG_THEO_LOI_CHOI[tl.loiChoi]
  const trongLuongMuon = tl.trongLuong && tl.trongLuong !== 'khongRo' ? [tl.trongLuong] : TRONG_LUONG_THEO_TRINH_DO[tl.trinhDo] || []
  const hangs = (tl.thuongHieu || []).map(boDau)

  const cham = (p) => {
    const lyDo = []
    let diem = 0
    if (doCung.includes(boDau(p.doCung))) {
      diem += 3
      lyDo.push(`Thân ${String(p.doCung).toLowerCase()} hợp ${tl.trinhDo === 'moi' ? 'người mới' : tl.trinhDo === 'nangCao' ? 'người chơi nâng cao' : 'chơi phong trào'}`)
    }
    if (tl.trinhDo === 'moi' && boDau(p.tenDanhMuc).includes('nguoi moi')) {
      diem += 1
      lyDo.push('Dòng vợt cho người mới')
    }
    if (canBang && boDau(p.diemCanBang) === canBang) {
      diem += 3
      lyDo.push(`${p.diemCanBang} - hợp lối ${tl.loiChoi === 'tanCong' ? 'tấn công' : tl.loiChoi === 'phongThu' ? 'phòng thủ' : 'toàn diện'}`)
    }
    const coTl = (p.bienThe || []).filter((b) => Number(b.soLuongTon) > 0).map((b) => maTrongLuong(b.trongLuong))
    const tlKhop = trongLuongMuon.filter((x) => coTl.includes(x))
    if (tlKhop.length) {
      diem += 2
      lyDo.push(`Còn hàng ${tlKhop.join(' / ')}`)
    }
    if (hangs.length && hangs.includes(boDau(p.tenThuongHieu))) {
      diem += 2
      lyDo.push(`Hãng ${p.tenThuongHieu}`)
    }
    if (Number(p.phanTramGiamMax) > 0) diem += 0.5
    return { sp: p, diem, lyDo, gia: giaThapNhat(p) }
  }

  const trongNganSach = conHang.map(cham).filter((x) => x.gia >= ns.tu && x.gia <= ns.den)
  const xep = (a, b) => b.diem - a.diem || a.gia - b.gia
  if (trongNganSach.length) return { ds: trongNganSach.sort(xep).slice(0, toiDa), ngoaiNganSach: false }
  // Không có mẫu nào trong ngân sách: gợi ý các mẫu có giá gần khoảng ngân sách nhất
  const cach = (g) => (g < ns.tu ? ns.tu - g : g > ns.den ? g - ns.den : 0)
  const gan = conHang.map(cham).sort((a, b) => cach(a.gia) - cach(b.gia) || b.diem - a.diem).slice(0, toiDa)
  return { ds: gan, ngoaiNganSach: true }
}
